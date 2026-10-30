package com.toneatti.turnos.catalogo.application.service;

import com.toneatti.turnos.catalogo.domain.model.ProfessionalCategory;
import com.toneatti.turnos.catalogo.domain.ports.in.GetAllProfessionalCategoriesUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

    @Mock
    private GetAllProfessionalCategoriesUseCase getAllProfessionalCategoriesUseCase;

    @InjectMocks
    private CatalogoService catalogoService;

    @Test
    void getAllProfessionalCategoriesReturnsTheCategoriesOfTheUseCase() {
        List<ProfessionalCategory> categories =
                List.of(new ProfessionalCategory(1L, "Clinica medica", "Atencion clinica general"));
        when(getAllProfessionalCategoriesUseCase.getAllProfessionalCategories()).thenReturn(categories);

        assertThat(catalogoService.getAllProfessionalCategories()).isSameAs(categories);
    }
}
