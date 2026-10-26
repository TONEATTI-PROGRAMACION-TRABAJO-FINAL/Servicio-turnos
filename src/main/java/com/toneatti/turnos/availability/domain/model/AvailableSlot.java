package com.toneatti.turnos.availability.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A free slot of a professional. Times are local to the place of attention.
 */
public record AvailableSlot(
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
) {
}
