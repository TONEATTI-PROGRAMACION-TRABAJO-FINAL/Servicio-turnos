package com.toneatti.turnos.catalogo.infrastructure.rest.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record CatalogoWeeklyScheduleResponse(
        Long id,
        Long professionalId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int slotDurationMinutes
) {
}
