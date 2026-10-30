package com.toneatti.turnos.catedra.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A reservation registered in the cátedra (section 11 of the integration contract). It exists from the initial
 * confirmation of the hold, as {@link AppointmentStatus#PHONE_PENDING}.
 */
public record Appointment(
        Long reservationId,
        String reservationProcessId,
        String externalPatientId,
        Long professionalId,
        LocalDate date,
        LocalTime startTime,
        AppointmentStatus status
) {
}
