package com.toneatti.turnos.catalogo.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record WeeklySchedule(
        Long id,
        Long professionalId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int slotDurationMinutes
) {
}
