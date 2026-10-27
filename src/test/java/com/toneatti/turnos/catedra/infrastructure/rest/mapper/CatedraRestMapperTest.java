package com.toneatti.turnos.catedra.infrastructure.rest.mapper;

import com.toneatti.turnos.catedra.domain.model.Occupancy;
import com.toneatti.turnos.catedra.domain.model.OccupancyStatus;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraOccupancyResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class CatedraRestMapperTest {

    private final CatedraRestMapper catedraRestMapper = new CatedraRestMapper();

    @Test
    void returnsNullForNullInput() {
        assertThat(catedraRestMapper.toDomainModel(null)).isNull();
    }

    @Test
    void mapsEveryFieldOfAnOccupancy() {
        CatedraOccupancyResponse response = new CatedraOccupancyResponse(LocalDate.of(2026, 9, 14),
                LocalTime.of(9, 30), OccupancyStatus.HELD, Instant.parse("2026-09-10T14:44:50Z"));

        Occupancy occupancy = catedraRestMapper.toDomainModel(response);

        assertThat(occupancy).usingRecursiveComparison().isEqualTo(response);
    }
}
