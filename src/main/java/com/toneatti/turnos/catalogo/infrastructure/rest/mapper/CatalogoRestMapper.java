package com.toneatti.turnos.catalogo.infrastructure.rest.mapper;

import com.toneatti.turnos.catalogo.domain.model.Professional;
import com.toneatti.turnos.catalogo.domain.model.ProfessionalCategory;
import com.toneatti.turnos.catalogo.infrastructure.rest.dto.CatalogoProfessionalCategoryResponse;
import com.toneatti.turnos.catalogo.infrastructure.rest.dto.CatalogoProfessionalResponse;
import org.springframework.stereotype.Component;

@Component
public class CatalogoRestMapper {

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
