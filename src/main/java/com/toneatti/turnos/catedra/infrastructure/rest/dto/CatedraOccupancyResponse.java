package com.toneatti.turnos.catedra.infrastructure.rest.dto;

import com.toneatti.turnos.catedra.domain.model.OccupancyStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record CatedraOccupancyResponse(
        LocalDate date,
        LocalTime startTime,
        OccupancyStatus status,
        Instant expiresAt
) {
}
