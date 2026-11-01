package com.toneatti.turnos.catedra.domain.model;

import java.time.Instant;

/**
 * A reservation the cátedra cancelled (section 12 of the integration contract). The cancellation of a reservation
 * already cancelled answers the same, with its original {@code cancelledAt}.
 */
public record AppointmentCancellation(
        Long reservationId,
        String reservationProcessId,
        Instant cancelledAt
) {
}
