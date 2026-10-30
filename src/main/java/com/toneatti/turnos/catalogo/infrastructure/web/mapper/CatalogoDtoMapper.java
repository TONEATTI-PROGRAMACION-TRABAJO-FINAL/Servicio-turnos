package com.toneatti.turnos.catalogo.infrastructure.web.mapper;

import com.toneatti.turnos.catalogo.domain.model.ProfessionalCategory;
import com.toneatti.turnos.catalogo.infrastructure.web.dto.ProfessionalCategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class CatalogoDtoMapper {

    public ProfessionalCategoryResponse toResponse(ProfessionalCategory domain) {
        if (domain == null) {
            return null;
        }
        return ProfessionalCategoryResponse.builder()
                .id(domain.id())
                .name(domain.name())
                .description(domain.description())
                .build();
    }
}
