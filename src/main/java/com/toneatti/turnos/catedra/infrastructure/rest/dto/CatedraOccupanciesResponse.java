package com.toneatti.turnos.catedra.infrastructure.rest.dto;

import java.time.LocalDate;
import java.util.List;

public record CatedraOccupanciesResponse(
        Long professionalId,
        LocalDate from,
        LocalDate to,
        List<CatedraOccupancyResponse> occupancies
) {
}
