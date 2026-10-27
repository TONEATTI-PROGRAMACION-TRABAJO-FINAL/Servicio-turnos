package com.toneatti.turnos.reservation.domain.ports.in;

import com.toneatti.turnos.reservation.domain.model.StartedReservation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public interface StartReservationUseCase {
    Optional<StartedReservation> startReservation(String userExternalId, Long professionalId, LocalDate date,
                                                  LocalTime startTime);
}
