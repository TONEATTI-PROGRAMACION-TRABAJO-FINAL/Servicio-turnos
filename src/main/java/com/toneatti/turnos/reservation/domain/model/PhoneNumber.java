package com.toneatti.turnos.reservation.domain.model;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A phone number the cátedra accepts (section 15.5 of the integration contract): without spaces, dashes, parentheses
 * or dots, an optional leading + and 7 to 15 digits. The constructor only takes a value already normalized;
 * {@link #parse} normalizes what the user typed first.
 */
public record PhoneNumber(String value) {

    private static final Pattern SEPARATORS = Pattern.compile("[\\s\\-().]");
    private static final Pattern VALID = Pattern.compile("^\\+?[0-9]{7,15}$");

    public PhoneNumber {
        if (value == null || !VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("Not a normalized phone number");
        }
    }

    /**
     * Removes the separators the cátedra removes before validating, and returns empty when what remains is not valid.
     */
    public static Optional<PhoneNumber> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String normalized = SEPARATORS.matcher(raw).replaceAll("");
        if (!VALID.matcher(normalized).matches()) {
            return Optional.empty();
        }
        return Optional.of(new PhoneNumber(normalized));
    }

    // Personal data: the generated toString would expose it in logs.
    @Override
    public String toString() {
        return "PhoneNumber[****]";
    }
}
