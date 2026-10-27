package com.toneatti.turnos.catalogo.infrastructure.rest.mapper;

import com.toneatti.turnos.catalogo.domain.model.Professional;
import com.toneatti.turnos.catalogo.domain.model.ProfessionalCategory;
import com.toneatti.turnos.catalogo.domain.model.WeeklySchedule;
import com.toneatti.turnos.catalogo.infrastructure.rest.dto.CatalogoProfessionalCategoryResponse;
import com.toneatti.turnos.catalogo.infrastructure.rest.dto.CatalogoProfessionalResponse;
import com.toneatti.turnos.catalogo.infrastructure.rest.dto.CatalogoWeeklyScheduleResponse;
import org.springframework.stereotype.Component;

@Component
public class CatalogoRestMapper {

    public WeeklySchedule toDomainModel(CatalogoWeeklyScheduleResponse response) {
        if (response == null) {
            return null;
        }
        return new WeeklySchedule(
                response.id(),
                response.professionalId(),
                response.dayOfWeek(),
                response.startTime(),
                response.endTime(),
                response.slotDurationMinutes());
    }

    public Professional toDomainModel(CatalogoProfessionalResponse response) {
        if (response == null) {
            return null;
        }
        return new Professional(
                response.id(),
                response.categoryId(),
                response.firstName(),
                response.lastName(),
                response.enabled());
    }

    public ProfessionalCategory toDomainModel(CatalogoProfessionalCategoryResponse response) {
        if (response == null) {
            return null;
        }
        return new ProfessionalCategory(
                response.id(),
                response.name(),
                response.description());
    }
}
