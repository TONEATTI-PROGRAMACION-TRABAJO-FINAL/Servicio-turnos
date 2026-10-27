package com.toneatti.turnos.catedra.infrastructure.rest.dto;

import java.time.Instant;

/**
 * The {@code status} of the response is always WAITING_FOR_PHONE, so it is not read.
 */
public record CatedraHoldConfirmationResponse(
        String reservationProcessId,
        Instant expiresAt
) {
}
