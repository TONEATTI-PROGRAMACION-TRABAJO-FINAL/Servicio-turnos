package com.toneatti.turnos.catalogo.infrastructure.rest.mapper;

import com.toneatti.turnos.catalogo.domain.model.Professional;
import com.toneatti.turnos.catalogo.domain.model.ProfessionalCategory;
import com.toneatti.turnos.catalogo.infrastructure.rest.dto.CatalogoProfessionalCategoryResponse;
import com.toneatti.turnos.catalogo.infrastructure.rest.dto.CatalogoProfessionalResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogoRestMapperTest {

    private final CatalogoRestMapper catalogoRestMapper = new CatalogoRestMapper();

    @Test
    void returnsNullForNullInput() {
        assertThat(catalogoRestMapper.toDomainModel((CatalogoProfessionalCategoryResponse) null)).isNull();
        assertThat(catalogoRestMapper.toDomainModel((CatalogoProfessionalResponse) null)).isNull();
    }

    @Test
    void mapsEveryFieldOfAProfessional() {
        CatalogoProfessionalResponse response = new CatalogoProfessionalResponse(15L, 1L, "Ana", "Gomez", true);

        Professional professional = catalogoRestMapper.toDomainModel(response);

        assertThat(professional).usingRecursiveComparison().isEqualTo(response);
    }

    @Test
    void mapsEveryFieldOfACategory() {
        CatalogoProfessionalCategoryResponse response =
                new CatalogoProfessionalCategoryResponse(1L, "Clinica medica", "Atencion clinica general");

        ProfessionalCategory category = catalogoRestMapper.toDomainModel(response);

        assertThat(category).usingRecursiveComparison().isEqualTo(response);
    }
}
