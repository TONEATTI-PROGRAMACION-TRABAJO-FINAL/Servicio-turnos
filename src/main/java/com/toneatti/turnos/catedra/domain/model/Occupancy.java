package com.toneatti.turnos.catedra.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A slot that is not available: a confirmed reservation or an active hold.
 * {@code expiresAt} only has a value for {@link OccupancyStatus#HELD}.
 */
public record Occupancy(
        LocalDate date,
        LocalTime startTime,
        OccupancyStatus status,
        Instant expiresAt
) {
}
