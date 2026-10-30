package com.toneatti.turnos.reservation.application.service;

import com.toneatti.turnos.reservation.application.exception.ReservationException;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction;
import com.toneatti.turnos.reservation.domain.model.AppointmentActionOutcome;
import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.model.StartedReservation;
import com.toneatti.turnos.reservation.domain.ports.in.ApplyAppointmentActionUseCase;
import com.toneatti.turnos.reservation.domain.ports.in.GetReservationByIdUseCase;
import com.toneatti.turnos.reservation.domain.ports.in.StartReservationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final StartReservationUseCase startReservationUseCase;
    private final GetReservationByIdUseCase getReservationByIdUseCase;
    private final ApplyAppointmentActionUseCase applyAppointmentActionUseCase;

    public StartedReservation startReservation(String userExternalId, Long professionalId, LocalDate date,
                                               LocalTime startTime) {
        return startReservationUseCase.startReservation(userExternalId, professionalId, date, startTime)
                .orElseThrow(() -> new ReservationException("Could not find user with externalId: " + userExternalId));
    }

    public ReservationProcess getReservationById(String userExternalId, Long id) {
        return getReservationByIdUseCase.getReservationById(userExternalId, id)
                .orElseThrow(() -> new ReservationException("Could not find reservation process with id: " + id));
    }

    public AppointmentActionOutcome applyAppointmentAction(AppointmentAction action) {
        return applyAppointmentActionUseCase.applyAppointmentAction(action);
    }

}
