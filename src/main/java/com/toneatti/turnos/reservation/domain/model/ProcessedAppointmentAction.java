package com.toneatti.turnos.reservation.domain.model;

import java.time.Instant;

/**
 * An event of the cátedra already processed, whatever its outcome: its {@code eventId} is not processed again.
 */
public record ProcessedAppointmentAction(
        Long id,
        String eventId,
        String eventType,
        String reservationProcessId,
        AppointmentActionOutcome outcome,
        Instant processedAt
) {
}
