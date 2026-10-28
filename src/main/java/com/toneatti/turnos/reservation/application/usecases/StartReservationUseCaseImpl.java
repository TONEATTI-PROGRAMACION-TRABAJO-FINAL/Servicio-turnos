package com.toneatti.turnos.reservation.application.usecases;

import com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException;
import com.toneatti.turnos.catedra.domain.exception.CatedraUnavailableException;
import com.toneatti.turnos.catedra.domain.model.AppointmentStatus;
import com.toneatti.turnos.catedra.domain.model.Hold;
import com.toneatti.turnos.catedra.domain.model.HoldConfirmation;
import com.toneatti.turnos.catedra.domain.model.Occupancy;
import com.toneatti.turnos.catedra.domain.model.OccupancyStatus;
import com.toneatti.turnos.catedra.domain.ports.out.CatedraRepository;
import com.toneatti.turnos.reservation.application.exception.HoldExpiredException;
import com.toneatti.turnos.reservation.application.exception.ReservationPendingException;
import com.toneatti.turnos.reservation.application.exception.ReservationRejectedException;
import com.toneatti.turnos.reservation.application.exception.ReservationUnavailableException;
import com.toneatti.turnos.reservation.application.exception.SlotInThePastException;
import com.toneatti.turnos.reservation.application.exception.UnexpectedCatedraRejectionException;
import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.model.StartedReservation;
import com.toneatti.turnos.reservation.domain.ports.in.StartReservationUseCase;
import com.toneatti.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.domain.ports.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;

import static com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException.HOLD_EXPIRED;
import static com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException.HOLD_NOT_FOUND;
import static com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException.INVALID_SLOT;
import static com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException.PROFESSIONAL_DISABLED;
import static com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException.PROFESSIONAL_NOT_FOUND;
import static com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException.RESERVATION_PROCESS_ALREADY_CONFIRMED;
import static com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException.SLOT_ALREADY_HELD;
import static com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException.SLOT_ALREADY_RESERVED;
import static com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException.STUDENT_INTEGRATION_NOT_FOUND;

/**
 * Holds the slot in the cátedra and confirms the hold right away (sections 9 and 10 of the integration contract):
 * the app has nothing to add between both steps. The patient is always the authenticated user: the
 * {@code externalPatientId} is its external id and the names come from its registration, never from the app.
 * Each status is saved before the call to the cátedra that leaves it, and no database transaction stays open
 * during those calls.
 * A call without an answer (timeout, server error) does not prove the operation failed (section 18.4): before
 * calling again, the outcome is reconciled through the occupancies (hold) or the reservations (confirmation) of the
 * cátedra, and each request makes at most {@value #MAX_ATTEMPTS} calls of each kind. When the outcome stays unknown
 * the process remains active, and starting the same reservation again resumes it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartReservationUseCaseImpl implements StartReservationUseCase {

    static final int MAX_ATTEMPTS = 2;

    // Rejections the user can act on (another slot, another professional). Any other code is our bug.
    private static final Set<String> USER_FACING_CODES = Set.of(PROFESSIONAL_NOT_FOUND, PROFESSIONAL_DISABLED,
            INVALID_SLOT, SLOT_ALREADY_RESERVED, SLOT_ALREADY_HELD, HOLD_NOT_FOUND);

    private final ReservationProcessRepository reservationProcessRepository;
    private final CatedraRepository catedraRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Override
    public Optional<StartedReservation> startReservation(String userExternalId, Long professionalId, LocalDate date,
                                                         LocalTime startTime) {
        Optional<User> user = userRepository.findByExternalId(userExternalId);
        if (user.isEmpty()) {
            return Optional.empty();
        }
        if (LocalDateTime.of(date, startTime).isBefore(LocalDateTime.now(clock))) {
            throw new SlotInThePastException(date, startTime);
        }
        Optional<ReservationProcess> active = reservationProcessRepository.findActiveByUserAndSlot(
                userExternalId, professionalId, date, startTime);
        if (active.isPresent()) {
            return Optional.of(new StartedReservation(resume(active.get(), user.get()), true));
        }
        ReservationProcess process = reservationProcessRepository.create(
                ReservationProcess.start(userExternalId, professionalId, date, startTime, clock.instant()));
        return Optional.of(new StartedReservation(confirm(hold(process, false), user.get(), false), false));
    }

    // A double tap or a retry of the app continues the process of the slot instead of holding it twice. The last
    // call of PENDING_HOLD and CONFIRMING got no answer, so they are reconciled before calling again. A confirmed
    // reservation of the slot is already the user's.
    private ReservationProcess resume(ReservationProcess process, User user) {
        return switch (process.getStatus()) {
            case WAITING_FOR_PHONE, CONFIRMED -> process;
            case PENDING_HOLD -> confirm(hold(process, true), user, false);
            case HELD -> confirm(process, user, false);
            case CONFIRMING -> confirm(process, user, true);
            case CANCELLED, EXPIRED, FAILED ->
                    throw new IllegalStateException("Process " + process.getId() + " is not active");
        };
    }

    // outcomeUnknown: an earlier call got no answer, so the hold may exist already.
    private ReservationProcess hold(ReservationProcess process, boolean outcomeUnknown) {
        CatedraUnavailableException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            if (outcomeUnknown) {
                ensureTheSlotHasNoHold(process, lastFailure);
            }
            try {
                Hold hold = catedraRepository.createHold(process.getProfessionalId(), process.getDate(),
                        process.getStartTime(), process.getUserExternalId());
                process.markHeld(hold.holdId(), hold.reservationProcessId(), hold.expiresAt(), clock.instant());
                return save(process);
            } catch (CatedraRequestRejectedException e) {
                throw fail(process, e);
            } catch (CatedraUnavailableException e) {
                logNoAnswer(process, "creating the hold", attempt, e);
                outcomeUnknown = true;
                lastFailure = e;
            }
        }
        throw new ReservationPendingException(process.getId(), lastFailure);
    }

    // The contract has no way to read a hold, so the occupancy of the slot is the only evidence (section 8). A free
    // slot proves the hold was not created and it can be requested again without duplicating it. A held slot may be
    // ours or someone else's: requesting again would answer SLOT_ALREADY_HELD either way, so the process stays
    // pending until the cátedra releases the slot at the expiresAt of that hold.
    private void ensureTheSlotHasNoHold(ReservationProcess process, CatedraUnavailableException cause) {
        Optional<Occupancy> occupancy;
        try {
            occupancy = catedraRepository.findOccupancies(process.getProfessionalId(), process.getDate(),
                            process.getDate()).stream()
                    .filter(slot -> slot.date().equals(process.getDate())
                            && slot.startTime().equals(process.getStartTime()))
                    .findFirst();
        } catch (CatedraUnavailableException | CatedraRequestRejectedException e) {
            log.warn("Could not reconcile the hold of reservation process {}", process.getId(), e);
            throw new ReservationPendingException(process.getId(), e);
        }
        if (occupancy.isEmpty()) {
            log.info("Slot of reservation process {} is free: requesting the hold again", process.getId());
            return;
        }
        if (occupancy.get().status() == OccupancyStatus.CONFIRMED) {
            process.markFailed(SLOT_ALREADY_RESERVED, clock.instant());
            save(process);
            throw new ReservationRejectedException(SLOT_ALREADY_RESERVED, cause);
        }
        log.warn("Slot of reservation process {} is held until {} and the hold may be ours: it stays pending",
                process.getId(), occupancy.get().expiresAt());
        throw new ReservationPendingException(process.getId(), cause);
    }

    // outcomeUnknown: an earlier confirmation got no answer, so it may have reached the cátedra.
    private ReservationProcess confirm(ReservationProcess process, User user, boolean outcomeUnknown) {
        CatedraUnavailableException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            // The hold only lives until expiresAt (section 9): confirming it later can only fail.
            if (process.isExpiredAt(clock.instant())) {
                throw expire(process);
            }
            if (outcomeUnknown) {
                if (theConfirmationArrived(process)) {
                    process.markWaitingForPhone(null, clock.instant());
                    return save(process);
                }
            } else {
                process.markConfirming(clock.instant());
                process = save(process);
            }
            try {
                HoldConfirmation confirmation = catedraRepository.confirmHold(process.getHoldId(),
                        process.getUserExternalId(), user.firstName(), user.lastName());
                process.markWaitingForPhone(confirmation.expiresAt(), clock.instant());
                return save(process);
            } catch (CatedraRequestRejectedException e) {
                if (RESERVATION_PROCESS_ALREADY_CONFIRMED.equals(e.getCode())) {
                    // An earlier confirmation of this same process reached the cátedra.
                    process.markWaitingForPhone(null, clock.instant());
                    return save(process);
                }
                if (HOLD_EXPIRED.equals(e.getCode())) {
                    throw expire(process);
                }
                throw fail(process, e);
            } catch (CatedraUnavailableException e) {
                logNoAnswer(process, "confirming the hold", attempt, e);
                outcomeUnknown = true;
                lastFailure = e;
            }
        }
        throw new ReservationPendingException(process.getId(), lastFailure);
    }

    // The cátedra creates the reservation PHONE_PENDING when it accepts the confirmation (section 10), and it can only
    // be found by patient and date (section 11). If it is not there, confirming again cannot duplicate it: a
    // confirmation that arrives in between is answered with RESERVATION_PROCESS_ALREADY_CONFIRMED.
    private boolean theConfirmationArrived(ReservationProcess process) {
        String reservationProcessId = process.getReservationProcessId();
        try {
            boolean arrived = catedraRepository.findAppointments(process.getUserExternalId(), process.getDate(),
                            process.getDate()).stream()
                    .anyMatch(appointment -> reservationProcessId.equals(appointment.reservationProcessId())
                            && (appointment.status() == AppointmentStatus.PHONE_PENDING
                            || appointment.status() == AppointmentStatus.CONFIRMED));
            log.info("Confirmation of reservation process {} {} the catedra", process.getId(),
                    arrived ? "reached" : "did not reach");
            return arrived;
        } catch (CatedraUnavailableException | CatedraRequestRejectedException e) {
            log.warn("Could not reconcile the confirmation of reservation process {}", process.getId(), e);
            throw new ReservationPendingException(process.getId(), e);
        }
    }

    private RuntimeException expire(ReservationProcess process) {
        process.markExpired(clock.instant());
        save(process);
        return new HoldExpiredException(process.getId());
    }

    // A rejection with a code is final: the cátedra did not apply the operation, so there is nothing to reconcile.
    private RuntimeException fail(ReservationProcess process, CatedraRequestRejectedException e) {
        process.markFailed(e.getCode(), clock.instant());
        save(process);
        if (USER_FACING_CODES.contains(e.getCode())) {
            return new ReservationRejectedException(e.getCode(), e);
        }
        if (STUDENT_INTEGRATION_NOT_FOUND.equals(e.getCode())) {
            return new ReservationUnavailableException("Catedra did not find the technical integration", e);
        }
        return new UnexpectedCatedraRejectionException(e.getCode(), e);
    }

    private static void logNoAnswer(ReservationProcess process, String operation, int attempt,
                                    CatedraUnavailableException e) {
        log.warn("No answer from the catedra {} of reservation process {} in {} (attempt {} of {})",
                operation, process.getId(), process.getStatus(), attempt, MAX_ATTEMPTS, e);
    }

    private ReservationProcess save(ReservationProcess process) {
        return reservationProcessRepository.update(process.getId(), process)
                .orElseThrow(() -> new IllegalStateException("Reservation process " + process.getId() + " vanished"));
    }
}
