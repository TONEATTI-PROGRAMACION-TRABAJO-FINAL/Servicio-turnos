package com.toneatti.turnos.catedra.infrastructure.rest.dto;

import com.toneatti.turnos.catedra.domain.model.AppointmentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Every field of a reservation of section 11, for the paginated listing the app sees.
 */
public record CatedraAppointmentListResponse(
        Long reservationId,
        String reservationProcessId,
        String externalPatientId,
        Long professionalId,
        String professionalFirstName,
        String professionalLastName,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        String patientFirstName,
        String patientLastName,
        String patientPhone,
        AppointmentStatus status,
        Instant createdAt,
        Instant confirmedAt,
        Instant cancelledAt,
        String cancellationReason
) {
}
