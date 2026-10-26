package com.toneatti.turnos.availability.infrastructure.web.mapper;

import com.toneatti.turnos.availability.domain.model.AvailableSlot;
import com.toneatti.turnos.availability.infrastructure.web.dto.AvailableSlotResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityDtoMapperTest {

    private final AvailabilityDtoMapper availabilityDtoMapper = new AvailabilityDtoMapper();

    @Test
    void returnsNullForNullInput() {
        assertThat(availabilityDtoMapper.toResponse(null)).isNull();
    }

    @Test
    void formatsTheDateAndTheTimesWithoutSeconds() {
        AvailableSlot slot = new AvailableSlot(LocalDate.of(2026, 9, 4), LocalTime.of(9, 0), LocalTime.of(9, 30));

        AvailableSlotResponse response = availabilityDtoMapper.toResponse(slot);

        assertThat(response.getDate()).isEqualTo("2026-09-04");
        assertThat(response.getStartTime()).isEqualTo("09:00");
        assertThat(response.getEndTime()).isEqualTo("09:30");
    }
}
