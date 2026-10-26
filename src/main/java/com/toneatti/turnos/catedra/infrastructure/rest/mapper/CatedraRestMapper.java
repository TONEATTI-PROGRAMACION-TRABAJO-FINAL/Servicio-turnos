package com.toneatti.turnos.catedra.infrastructure.rest.mapper;

import com.toneatti.turnos.catedra.domain.model.Occupancy;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraOccupancyResponse;
import org.springframework.stereotype.Component;

@Component
public class CatedraRestMapper {

    public Occupancy toDomainModel(CatedraOccupancyResponse response) {
        if (response == null) {
            return null;
        }
        return new Occupancy(
                response.date(),
                response.startTime(),
                response.status(),
                response.expiresAt());
    }
}
