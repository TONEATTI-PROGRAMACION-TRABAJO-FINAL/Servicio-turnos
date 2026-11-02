package com.toneatti.turnos.catedra.infrastructure.rest.dto;

import com.toneatti.turnos.catedra.domain.model.AppointmentStatus;

import java.time.Instant;

/**
 * The {@code status} is always CANCELLED; it is only read to reject an answer that does not say so.
 */
public record CatedraCancellationResponse(
        Long reservationId,
        String reservationProcessId,
        AppointmentStatus status,
        Instant cancelledAt
) {
}
