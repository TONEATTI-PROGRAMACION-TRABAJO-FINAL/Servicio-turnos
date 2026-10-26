package com.toneatti.turnos.availability.application.exception;

import java.time.LocalDate;

public class DateRangeTooLongException extends RuntimeException {
    public DateRangeTooLongException(LocalDate from, LocalDate to, int maxDays) {
        super("The range from " + from + " to " + to + " is longer than " + maxDays + " days");
    }
}
