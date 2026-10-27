package com.toneatti.turnos.catedra.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public record CatedraHoldRequest(
        Long professionalId,
        LocalDate date,
        // Sent as HH:mm, the format of the example in section 9.
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        String externalPatientId
) {
}
