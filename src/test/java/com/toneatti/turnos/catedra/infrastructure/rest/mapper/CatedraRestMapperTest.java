package com.toneatti.turnos.catedra.infrastructure.rest.mapper;

import com.toneatti.turnos.catedra.domain.model.Hold;
import com.toneatti.turnos.catedra.domain.model.HoldConfirmation;
import com.toneatti.turnos.catedra.domain.model.Occupancy;
import com.toneatti.turnos.catedra.domain.model.OccupancyStatus;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraHoldConfirmationResponse;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraHoldResponse;
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
        assertThat(catedraRestMapper.toDomainModel((CatedraOccupancyResponse) null)).isNull();
        assertThat(catedraRestMapper.toDomainModel((CatedraHoldResponse) null)).isNull();
        assertThat(catedraRestMapper.toDomainModel((CatedraHoldConfirmationResponse) null)).isNull();
    }

    @Test
    void mapsEveryFieldOfAnOccupancy() {
        CatedraOccupancyResponse response = new CatedraOccupancyResponse(LocalDate.of(2026, 9, 14),
                LocalTime.of(9, 30), OccupancyStatus.HELD, Instant.parse("2026-09-10T14:44:50Z"));

        Occupancy occupancy = catedraRestMapper.toDomainModel(response);

        assertThat(occupancy).usingRecursiveComparison().isEqualTo(response);
    }

    @Test
    void mapsEveryFieldOfAHold() {
        CatedraHoldResponse response = new CatedraHoldResponse("hold-6c0c2f41", "process-a92b47d0",
                Instant.parse("2026-09-10T14:44:50Z"));

        Hold hold = catedraRestMapper.toDomainModel(response);

        assertThat(hold).usingRecursiveComparison().isEqualTo(response);
    }

    @Test
    void mapsEveryFieldOfAHoldConfirmation() {
        CatedraHoldConfirmationResponse response = new CatedraHoldConfirmationResponse("process-a92b47d0",
                Instant.parse("2026-09-10T14:44:50Z"));

        HoldConfirmation confirmation = catedraRestMapper.toDomainModel(response);

        assertThat(confirmation).usingRecursiveComparison().isEqualTo(response);
    }
}
