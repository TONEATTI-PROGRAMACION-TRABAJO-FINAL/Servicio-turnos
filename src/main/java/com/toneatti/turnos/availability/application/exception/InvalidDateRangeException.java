package com.toneatti.turnos.availability.application.exception;

import java.time.LocalDate;

/**
 * {@code from} is after {@code to}; the same rule as INVALID_DATE_RANGE of the cátedra.
 */
public class InvalidDateRangeException extends RuntimeException {
    public InvalidDateRangeException(LocalDate from, LocalDate to) {
        super("from " + from + " is after to " + to);
    }
}
