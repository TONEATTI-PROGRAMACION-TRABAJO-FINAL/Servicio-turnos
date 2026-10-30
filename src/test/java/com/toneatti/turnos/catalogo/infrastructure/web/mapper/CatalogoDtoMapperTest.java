package com.toneatti.turnos.catalogo.infrastructure.web.mapper;

import com.toneatti.turnos.catalogo.domain.model.ProfessionalCategory;
import com.toneatti.turnos.catalogo.infrastructure.web.dto.ProfessionalCategoryResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogoDtoMapperTest {

    private final CatalogoDtoMapper catalogoDtoMapper = new CatalogoDtoMapper();

    @Test
    void returnsNullForANullCategory() {
        assertThat(catalogoDtoMapper.toResponse((ProfessionalCategory) null)).isNull();
    }

    @Test
    void copiesEveryFieldOfTheCategory() {
        ProfessionalCategoryResponse response = catalogoDtoMapper.toResponse(
                new ProfessionalCategory(1L, "Clinica medica", "Atencion clinica general"));

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Clinica medica");
        assertThat(response.getDescription()).isEqualTo("Atencion clinica general");
    }
}
