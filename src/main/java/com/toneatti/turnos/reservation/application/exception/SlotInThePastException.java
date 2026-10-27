package com.toneatti.turnos.reservation.application.exception;

import java.time.LocalDate;
import java.time.LocalTime;

public class SlotInThePastException extends RuntimeException {
    public SlotInThePastException(LocalDate date, LocalTime startTime) {
        super("The slot of " + date + " at " + startTime + " already started");
    }
}
