package com.toneatti.turnos.user.domain.model;

import java.time.Instant;

public record AccessToken(
        String value,
        Instant issuedAt,
        Instant expiresAt
) {

    // The generated toString would expose the token in logs.
    @Override
    public String toString() {
        return "AccessToken[issuedAt=" + issuedAt + ", expiresAt=" + expiresAt + "]";
    }
}
