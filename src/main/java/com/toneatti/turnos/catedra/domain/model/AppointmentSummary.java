package com.toneatti.turnos.catedra.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A reservation registered in the cátedra (section 11 of the integration contract), with every field a listing
 * for the app needs: the professional's and patient's names and the lifecycle timestamps that {@link Appointment}
 * does not carry.
 */
public record AppointmentSummary(
        Long reservationId,
        String reservationProcessId,
        String externalPatientId,
        Long professionalId,
        String professionalFirstName,
        String professionalLastName,
        LocalDate date,
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
