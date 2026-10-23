package com.toneatti.turnos.catalogo.infrastructure.rest.mapper;

import com.toneatti.turnos.catalogo.domain.model.ProfessionalCategory;
import com.toneatti.turnos.catalogo.infrastructure.rest.dto.CatalogoProfessionalCategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class CatalogoRestMapper {

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
