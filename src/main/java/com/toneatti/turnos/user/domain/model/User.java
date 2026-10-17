package com.toneatti.turnos.user.domain.model;

import java.time.Instant;

public record User(
        Long id,
        String externalId,
        String email,
        String passwordHash,
        String firstName,
        String lastName,
        Instant createdAt
) {

    // The generated toString would expose the password hash in logs.
    @Override
    public String toString() {
        return "User[id=" + id + ", externalId=" + externalId + ", email=" + email
                + ", firstName=" + firstName + ", lastName=" + lastName + ", createdAt=" + createdAt + "]";
    }
}
