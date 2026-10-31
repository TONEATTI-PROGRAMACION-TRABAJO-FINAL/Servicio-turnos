package com.toneatti.turnos.catalogo.application.service;

import com.toneatti.turnos.catalogo.application.exception.CatalogoException;
import com.toneatti.turnos.catalogo.domain.model.Professional;
import com.toneatti.turnos.catalogo.domain.model.ProfessionalCategory;
import com.toneatti.turnos.catalogo.domain.ports.in.GetAllProfessionalCategoriesUseCase;
import com.toneatti.turnos.catalogo.domain.ports.in.GetProfessionalByIdUseCase;
import com.toneatti.turnos.catalogo.domain.ports.in.GetProfessionalsByCategoryUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

    @Mock
    private GetAllProfessionalCategoriesUseCase getAllProfessionalCategoriesUseCase;

    @Mock
    private GetProfessionalsByCategoryUseCase getProfessionalsByCategoryUseCase;

    @Mock
    private GetProfessionalByIdUseCase getProfessionalByIdUseCase;

    @InjectMocks
    private CatalogoService catalogoService;

    @Test
    void getAllProfessionalCategoriesReturnsTheCategoriesOfTheUseCase() {
        List<ProfessionalCategory> categories =
                List.of(new ProfessionalCategory(1L, "Clinica medica", "Atencion clinica general"));
        when(getAllProfessionalCategoriesUseCase.getAllProfessionalCategories()).thenReturn(categories);

        assertThat(catalogoService.getAllProfessionalCategories()).isSameAs(categories);
    }

    @Test
    void getProfessionalsByCategoryReturnsTheProfessionalsOfTheUseCase() {
        List<Professional> professionals = List.of(new Professional(15L, 1L, "Ana", "Gomez", true));
        when(getProfessionalsByCategoryUseCase.getProfessionalsByCategory(1L)).thenReturn(professionals);

        assertThat(catalogoService.getProfessionalsByCategory(1L)).isSameAs(professionals);
    }

    @Test
    void getProfessionalByIdReturnsTheProfessionalOfTheUseCase() {
        Professional ana = new Professional(15L, 1L, "Ana", "Gomez", true);
        when(getProfessionalByIdUseCase.getProfessionalById(15L)).thenReturn(Optional.of(ana));

        assertThat(catalogoService.getProfessionalById(15L)).isSameAs(ana);
    }

    @Test
    void getProfessionalByIdThrowsCatalogoExceptionWhenTheProfessionalDoesNotExist() {
        when(getProfessionalByIdUseCase.getProfessionalById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogoService.getProfessionalById(99L))
                .isInstanceOf(CatalogoException.class)
                .hasMessageContaining("99");
    }
}
