package com.toneatti.turnos.reservation.application.usecases;

import com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException;
import com.toneatti.turnos.catedra.domain.exception.CatedraUnavailableException;
import com.toneatti.turnos.catedra.domain.model.Hold;
import com.toneatti.turnos.catedra.domain.model.HoldConfirmation;
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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartReservationUseCaseImpl implements StartReservationUseCase {

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
        return Optional.of(new StartedReservation(confirm(hold(process), user.get()), false));
    }

    // A double tap or a retry of the app continues the process of the slot instead of holding it twice.
    private ReservationProcess resume(ReservationProcess process, User user) {
        return switch (process.getStatus()) {
            case WAITING_FOR_PHONE -> process;
            case HELD -> confirm(process, user);
            // The last call to the cátedra may have had effects: repeating it blindly could duplicate them.
            case PENDING_HOLD, CONFIRMING -> throw new ReservationPendingException(process.getId(), null);
            case EXPIRED, FAILED -> throw new IllegalStateException("Process " + process.getId() + " is not active");
        };
    }

    private ReservationProcess hold(ReservationProcess process) {
        Hold hold;
        try {
            hold = catedraRepository.createHold(process.getProfessionalId(), process.getDate(),
                    process.getStartTime(), process.getUserExternalId());
        } catch (CatedraRequestRejectedException e) {
            throw fail(process, e);
        } catch (CatedraUnavailableException e) {
            throw pending(process, "creating the hold", e);
        }
        process.markHeld(hold.holdId(), hold.reservationProcessId(), hold.expiresAt(), clock.instant());
        return save(process);
    }

    private ReservationProcess confirm(ReservationProcess process, User user) {
        // The hold only lives until expiresAt (section 9): confirming it later can only fail.
        if (process.isExpiredAt(clock.instant())) {
            throw expire(process);
        }
        process.markConfirming(clock.instant());
        process = save(process);
        HoldConfirmation confirmation;
        try {
            confirmation = catedraRepository.confirmHold(process.getHoldId(), process.getUserExternalId(),
                    user.firstName(), user.lastName());
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
            throw pending(process, "confirming the hold", e);
        }
        process.markWaitingForPhone(confirmation.expiresAt(), clock.instant());
        return save(process);
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

    private RuntimeException pending(ReservationProcess process, String operation, CatedraUnavailableException e) {
        log.warn("No answer from the catedra {} of reservation process {} in {}",
                operation, process.getId(), process.getStatus(), e);
        return new ReservationPendingException(process.getId(), e);
    }

    private ReservationProcess save(ReservationProcess process) {
        return reservationProcessRepository.update(process.getId(), process)
                .orElseThrow(() -> new IllegalStateException("Reservation process " + process.getId() + " vanished"));
    }
}
