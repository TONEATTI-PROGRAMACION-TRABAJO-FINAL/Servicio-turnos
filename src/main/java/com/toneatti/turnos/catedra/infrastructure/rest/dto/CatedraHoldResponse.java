package com.toneatti.turnos.catedra.infrastructure.rest.dto;

import java.time.Instant;

/**
 * The {@code status} of the response is always HELD, so it is not read.
 */
public record CatedraHoldResponse(
        String holdId,
        String reservationProcessId,
        Instant expiresAt
) {
}
