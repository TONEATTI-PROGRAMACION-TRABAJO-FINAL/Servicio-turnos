package com.toneatti.turnos.reservation.application.usecases;

import com.toneatti.turnos.reservation.application.exception.InvalidPhoneNumberException;
import com.toneatti.turnos.reservation.application.exception.PhoneNotRequestedException;
import com.toneatti.turnos.reservation.application.exception.ReservationExpiredException;
import com.toneatti.turnos.reservation.application.exception.ReservationNotWaitingForPhoneException;
import com.toneatti.turnos.reservation.domain.model.PhoneNumber;
import com.toneatti.turnos.reservation.domain.model.PhoneSubmission;
import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.model.ReservationStatus;
import com.toneatti.turnos.reservation.domain.ports.in.SubmitPhoneUseCase;
import com.toneatti.turnos.reservation.domain.ports.out.PhoneSubmissionPublisher;
import com.toneatti.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Sends to the cátedra the phone number of a process of the user that is waiting for it (section 15.5 of the
 * integration contract). The number is validated as the cátedra does before anything else, and it is not stored:
 * it only travels in the event. The deadline is the expiresAt of the cátedra; an expired process is rejected but not
 * closed here, since the cátedra reports the end of the phone step (section 15.8).
 * The process is saved before publishing, without the rejection of the previous phone: if an event of the cátedra
 * changed it meanwhile, saving fails on its version and nothing is published. No database transaction stays open
 * while publishing. Each submission is a new event, also the one sent again after a rejection (section 15.7).
 */
@Component
@RequiredArgsConstructor
public class SubmitPhoneUseCaseImpl implements SubmitPhoneUseCase {

    private final ReservationProcessRepository reservationProcessRepository;
    private final PhoneSubmissionPublisher phoneSubmissionPublisher;
    private final Clock clock;

    @Override
    public Optional<ReservationProcess> submitPhone(String userExternalId, Long id, String phoneNumber) {
        PhoneNumber phone = PhoneNumber.parse(phoneNumber).orElseThrow(InvalidPhoneNumberException::new);
        return reservationProcessRepository.findByIdAndUserExternalId(id, userExternalId)
                .flatMap(process -> submit(process, phone));
    }

    private Optional<ReservationProcess> submit(ReservationProcess process, PhoneNumber phone) {
        Instant now = clock.instant();
        if (process.getStatus() != ReservationStatus.WAITING_FOR_PHONE) {
            throw new ReservationNotWaitingForPhoneException(process.getId(), process.getStatus());
        }
        if (process.getRequestEventId() == null) {
            throw new PhoneNotRequestedException(process.getId());
        }
        if (process.isExpiredAt(now)) {
            throw new ReservationExpiredException(process.getId());
        }
        process.submitPhone(now);
        Optional<ReservationProcess> saved = reservationProcessRepository.update(process.getId(), process);
        saved.ifPresent(current -> phoneSubmissionPublisher.publish(new PhoneSubmission(UUID.randomUUID().toString(),
                current.getReservationProcessId(), current.getHoldId(), current.getRequestEventId(), phone, now)));
        return saved;
    }
}
