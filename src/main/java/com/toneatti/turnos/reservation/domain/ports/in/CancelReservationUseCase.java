package com.toneatti.turnos.reservation.domain.ports.in;

import com.toneatti.turnos.reservation.domain.model.ReservationProcess;

import java.util.Optional;

public interface CancelReservationUseCase {
    Optional<ReservationProcess> cancelReservation(String userExternalId, String reservationProcessId, String reason);
}
