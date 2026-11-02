package com.toneatti.turnos.reservation.application.service;

import com.toneatti.turnos.catedra.domain.model.AppointmentPage;
import com.toneatti.turnos.catedra.domain.model.AppointmentStatus;
import com.toneatti.turnos.reservation.application.exception.ReservationException;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction;
import com.toneatti.turnos.reservation.domain.model.AppointmentActionOutcome;
import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.model.StartedReservation;
import com.toneatti.turnos.reservation.domain.ports.in.ApplyAppointmentActionUseCase;
import com.toneatti.turnos.reservation.domain.ports.in.CancelReservationUseCase;
import com.toneatti.turnos.reservation.domain.ports.in.GetReservationByIdUseCase;
import com.toneatti.turnos.reservation.domain.ports.in.ListReservationsUseCase;
import com.toneatti.turnos.reservation.domain.ports.in.StartReservationUseCase;
import com.toneatti.turnos.reservation.domain.ports.in.SubmitPhoneUseCase;
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
    private final SubmitPhoneUseCase submitPhoneUseCase;
    private final ListReservationsUseCase listReservationsUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;

    public StartedReservation startReservation(String userExternalId, Long professionalId, LocalDate date,
                                               LocalTime startTime) {
        return startReservationUseCase.startReservation(userExternalId, professionalId, date, startTime)
                .orElseThrow(() -> new ReservationException("Could not find user with externalId: " + userExternalId));
    }

    public ReservationProcess getReservationById(String userExternalId, Long id) {
        return getReservationByIdUseCase.getReservationById(userExternalId, id)
                .orElseThrow(() -> reservationNotFound(id));
    }

    public ReservationProcess submitPhone(String userExternalId, Long id, String phoneNumber) {
        return submitPhoneUseCase.submitPhone(userExternalId, id, phoneNumber)
                .orElseThrow(() -> reservationNotFound(id));
    }

    public AppointmentActionOutcome applyAppointmentAction(AppointmentAction action) {
        return applyAppointmentActionUseCase.applyAppointmentAction(action);
    }

    public AppointmentPage listReservations(String userExternalId, LocalDate from, LocalDate to,
                                            AppointmentStatus status, int page, int size) {
        return listReservationsUseCase.listReservations(userExternalId, from, to, status, page, size);
    }

    public ReservationProcess cancelReservation(String userExternalId, String reservationProcessId, String reason) {
        return cancelReservationUseCase.cancelReservation(userExternalId, reservationProcessId, reason)
                .orElseThrow(() -> new ReservationException(
                        "Could not find reservation process with reservationProcessId: " + reservationProcessId));
    }

    private static ReservationException reservationNotFound(Long id) {
        return new ReservationException("Could not find reservation process with id: " + id);
    }

}
