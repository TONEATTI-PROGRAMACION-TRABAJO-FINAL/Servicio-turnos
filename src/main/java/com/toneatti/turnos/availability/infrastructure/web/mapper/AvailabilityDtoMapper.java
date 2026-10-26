package com.toneatti.turnos.availability.infrastructure.web.mapper;

import com.toneatti.turnos.availability.domain.model.AvailableSlot;
import com.toneatti.turnos.availability.infrastructure.web.dto.AvailableSlotResponse;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class AvailabilityDtoMapper {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    public AvailableSlotResponse toResponse(AvailableSlot domain) {
        if (domain == null) {
            return null;
        }
        return AvailableSlotResponse.builder()
                .date(domain.date().toString())
                .startTime(domain.startTime().format(TIME))
                .endTime(domain.endTime().format(TIME))
                .build();
    }
}
