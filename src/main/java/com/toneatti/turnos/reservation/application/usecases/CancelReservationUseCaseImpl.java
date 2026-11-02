package com.toneatti.turnos.reservation.application.usecases;

import com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException;
import com.toneatti.turnos.catedra.domain.exception.CatedraUnavailableException;
import com.toneatti.turnos.catedra.domain.model.Appointment;
import com.toneatti.turnos.catedra.domain.model.AppointmentStatus;
import com.toneatti.turnos.catedra.domain.ports.out.CatedraRepository;
import com.toneatti.turnos.reservation.application.exception.CancellationPendingException;
import com.toneatti.turnos.reservation.application.exception.ConcurrentReservationUpdateException;
import com.toneatti.turnos.reservation.application.exception.ReservationNotCancellableException;
import com.toneatti.turnos.reservation.application.exception.ReservationUnavailableException;
import com.toneatti.turnos.reservation.application.exception.UnexpectedCatedraRejectionException;
import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.model.ReservationStatus;
import com.toneatti.turnos.reservation.domain.ports.in.CancelReservationUseCase;
import com.toneatti.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Optional;

import static com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException.RESERVATION_NOT_CANCELLABLE;
import static com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException.STUDENT_INTEGRATION_NOT_FOUND;

/**
 * Cancels a reservation of the user in the cátedra (section 12 of the integration contract). The owner is checked
 * against the local process before calling the cátedra, whose listing has the reservations of every user. Only a
 * CONFIRMED reservation is cancelled; one already CANCELLED, locally or by the AppointmentCancelled event, is
 * returned as it is, so cancelling again is idempotent.
 * A call without an answer does not prove the cancellation failed (section 18.4): before calling again, the
 * reservations of the patient for that day show whether it reached the cátedra, and each request makes at most
 * {@value #MAX_ATTEMPTS} calls. Calling again is safe either way, since the cátedra answers a cancelled reservation
 * with its current state. No database transaction stays open during those calls.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CancelReservationUseCaseImpl implements CancelReservationUseCase {

    static final int MAX_ATTEMPTS = 2;

    private final ReservationProcessRepository reservationProcessRepository;
    private final CatedraRepository catedraRepository;
    private final Clock clock;

    @Override
    public Optional<ReservationProcess> cancelReservation(String userExternalId, String reservationProcessId,
                                                          String reason) {
        String cancellationReason = reason == null || reason.isBlank() ? null : reason.strip();
        return reservationProcessRepository
                .findByReservationProcessIdAndUserExternalId(reservationProcessId, userExternalId)
                .map(process -> cancel(process, cancellationReason));
    }

    private ReservationProcess cancel(ReservationProcess process, String reason) {
        if (process.getStatus() == ReservationStatus.CANCELLED) {
            return process;
        }
        if (process.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ReservationNotCancellableException(process.getId(), process.getStatus());
        }
        CatedraUnavailableException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            if (lastFailure != null && theCancellationArrived(process, lastFailure)) {
                return markCancelled(process, reason);
            }
            try {
                catedraRepository.cancelAppointment(process.getReservationProcessId(), reason);
                return markCancelled(process, reason);
            } catch (CatedraRequestRejectedException e) {
                throw rejected(process, e);
            } catch (CatedraUnavailableException e) {
                log.warn("No answer from the catedra cancelling reservation process {} (attempt {} of {})",
                        process.getId(), attempt, MAX_ATTEMPTS, e);
                lastFailure = e;
            }
        }
        throw new CancellationPendingException(process.getId(), lastFailure);
    }

    // CANCELLED proves the cancellation arrived and CONFIRMED that it did not, so it can be requested again. Without
    // the reservation, or without an answer, the outcome stays unknown.
    private boolean theCancellationArrived(ReservationProcess process, CatedraUnavailableException cause) {
        Optional<AppointmentStatus> status;
        try {
            status = catedraRepository.findAppointments(process.getUserExternalId(), process.getDate(),
                            process.getDate()).stream()
                    .filter(appointment -> process.getReservationProcessId().equals(appointment.reservationProcessId()))
                    .map(Appointment::status)
                    .findFirst();
        } catch (CatedraUnavailableException | CatedraRequestRejectedException e) {
            log.warn("Could not reconcile the cancellation of reservation process {}", process.getId(), e);
            throw new CancellationPendingException(process.getId(), e);
        }
        if (status.isEmpty() || (status.get() != AppointmentStatus.CANCELLED
                && status.get() != AppointmentStatus.CONFIRMED)) {
            log.warn("Reservation process {} is {} in the catedra: the cancellation stays pending", process.getId(),
                    status.map(Enum::name).orElse("missing"));
            throw new CancellationPendingException(process.getId(), cause);
        }
        log.info("Cancellation of reservation process {} {} the catedra", process.getId(),
                status.get() == AppointmentStatus.CANCELLED ? "reached" : "did not reach");
        return status.get() == AppointmentStatus.CANCELLED;
    }

    // The AppointmentCancelled event may be applied before this request saves: saving then fails on the version, and
    // the process the event left is the outcome. The event that arrives after is ignored as late.
    private ReservationProcess markCancelled(ReservationProcess process, String reason) {
        process.markCancelled(reason, clock.instant());
        try {
            return reservationProcessRepository.update(process.getId(), process)
                    .orElseThrow(() -> new IllegalStateException("Reservation process " + process.getId()
                            + " vanished"));
        } catch (ConcurrentReservationUpdateException e) {
            ReservationProcess current = reservationProcessRepository
                    .findByReservationProcessIdAndUserExternalId(process.getReservationProcessId(),
                            process.getUserExternalId())
                    .filter(found -> found.getStatus() == ReservationStatus.CANCELLED)
                    .orElseThrow(() -> e);
            log.info("Reservation process {} was already cancelled by an event of the catedra", current.getId());
            return current;
        }
    }

    // A rejection with a code is final: the cátedra did not cancel, so the process stays as it is.
    private RuntimeException rejected(ReservationProcess process, CatedraRequestRejectedException e) {
        if (RESERVATION_NOT_CANCELLABLE.equals(e.getCode())) {
            log.warn("Catedra does not allow cancelling reservation process {}, which is CONFIRMED locally",
                    process.getId(), e);
            return new ReservationNotCancellableException(process.getId(), e);
        }
        if (STUDENT_INTEGRATION_NOT_FOUND.equals(e.getCode())) {
            return new ReservationUnavailableException("Catedra did not find the technical integration", e);
        }
        return new UnexpectedCatedraRejectionException(e.getCode(), e);
    }
}
