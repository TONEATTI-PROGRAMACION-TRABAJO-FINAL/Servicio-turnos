package com.toneatti.turnos.user.domain.model;

import java.time.Instant;
import java.util.Locale;

public record User(
        Long id,
        String externalId,
        String email,
        String passwordHash,
        String firstName,
        String lastName,
        Instant createdAt
) {

    // Emails are unique regardless of case and surrounding spaces.
    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    // The generated toString would expose the password hash in logs.
    @Override
    public String toString() {
        return "User[id=" + id + ", externalId=" + externalId + ", email=" + email
                + ", firstName=" + firstName + ", lastName=" + lastName + ", createdAt=" + createdAt + "]";
    }
}
