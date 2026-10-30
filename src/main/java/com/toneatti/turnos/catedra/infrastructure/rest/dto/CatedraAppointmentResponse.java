package com.toneatti.turnos.catedra.infrastructure.rest.dto;

import com.toneatti.turnos.catedra.domain.model.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * The fields of a reservation of section 11 that this service uses; the others are ignored.
 */
public record CatedraAppointmentResponse(
        Long reservationId,
        String reservationProcessId,
        String externalPatientId,
        Long professionalId,
        LocalDate appointmentDate,
        LocalTime startTime,
        AppointmentStatus status
) {
}
