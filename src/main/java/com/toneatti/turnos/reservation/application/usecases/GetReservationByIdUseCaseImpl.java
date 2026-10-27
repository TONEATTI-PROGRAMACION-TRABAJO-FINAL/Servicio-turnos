package com.toneatti.turnos.reservation.application.usecases;

import com.toneatti.turnos.reservation.application.exception.ConcurrentReservationUpdateException;
import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.model.ReservationStatus;
import com.toneatti.turnos.reservation.domain.ports.in.GetReservationByIdUseCase;
import com.toneatti.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Optional;

/**
 * Only the processes of the user: another user's process is reported as not found, so its existence is not revealed.
 * Reading never calls the cátedra; reconciling an unknown outcome is left to starting the reservation again.
 */
@Component
@RequiredArgsConstructor
public class GetReservationByIdUseCaseImpl implements GetReservationByIdUseCase {

    private final ReservationProcessRepository reservationProcessRepository;
    private final Clock clock;

    @Override
    public Optional<ReservationProcess> getReservationById(String userExternalId, Long id) {
        return reservationProcessRepository.findByIdAndUserExternalId(id, userExternalId)
                .map(process -> expireIfTheHoldExpired(process, userExternalId));
    }

    // A hold that was not confirmed in time is gone (section 9), so the app sees EXPIRED instead of a stale HELD.
    // WAITING_FOR_PHONE is left alone: the cátedra reports the end of that step (section 15.8).
    private ReservationProcess expireIfTheHoldExpired(ReservationProcess process, String userExternalId) {
        boolean holdOnly = process.getStatus() == ReservationStatus.HELD
                || process.getStatus() == ReservationStatus.CONFIRMING;
        if (!holdOnly || !process.isExpiredAt(clock.instant())) {
            return process;
        }
        process.markExpired(clock.instant());
        try {
            return reservationProcessRepository.update(process.getId(), process).orElse(process);
        } catch (ConcurrentReservationUpdateException e) {
            // Another request changed it meanwhile: its version is the current one.
            return reservationProcessRepository.findByIdAndUserExternalId(process.getId(), userExternalId)
                    .orElse(process);
        }
    }
}
