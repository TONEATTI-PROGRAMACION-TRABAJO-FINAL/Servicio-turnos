package com.toneatti.turnos.reservation.application.service;

import com.toneatti.turnos.reservation.application.exception.ReservationException;
import com.toneatti.turnos.reservation.domain.model.StartedReservation;
import com.toneatti.turnos.reservation.domain.ports.in.StartReservationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final StartReservationUseCase startReservationUseCase;

    public StartedReservation startReservation(String userExternalId, Long professionalId, LocalDate date,
                                               LocalTime startTime) {
        return startReservationUseCase.startReservation(userExternalId, professionalId, date, startTime)
                .orElseThrow(() -> new ReservationException("Could not find user with externalId: " + userExternalId));
    }

}
