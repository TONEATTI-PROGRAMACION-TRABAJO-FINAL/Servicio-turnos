package com.toneatti.turnos.reservation.domain.ports.in;

import com.toneatti.turnos.reservation.domain.model.ReservationProcess;

import java.util.Optional;

public interface GetReservationByIdUseCase {
    Optional<ReservationProcess> getReservationById(String userExternalId, Long id);
}
