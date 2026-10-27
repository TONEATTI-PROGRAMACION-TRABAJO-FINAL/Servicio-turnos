package com.toneatti.turnos.catedra.domain.model;

import java.time.Instant;

/**
 * The initial confirmation of a hold (section 10 of the integration contract): the cátedra created the reservation
 * PHONE_PENDING and now waits for the phone, which must arrive before {@code expiresAt}.
 */
public record HoldConfirmation(
        String reservationProcessId,
        Instant expiresAt
) {
}
