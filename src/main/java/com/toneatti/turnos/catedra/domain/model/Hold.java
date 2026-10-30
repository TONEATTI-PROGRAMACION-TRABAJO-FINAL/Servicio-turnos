package com.toneatti.turnos.catedra.domain.model;

import java.time.Instant;

/**
 * A temporary hold of a slot in the cátedra (section 9 of the integration contract). It lasts until
 * {@code expiresAt}; a local copy does not extend it.
 */
public record Hold(
        String holdId,
        String reservationProcessId,
        Instant expiresAt
) {
}
