package com.toneatti.turnos.catalogo.application.usecases;

import com.toneatti.turnos.catalogo.domain.exception.CatalogoUnavailableException;
import com.toneatti.turnos.catalogo.domain.model.ProfessionalCategory;
import com.toneatti.turnos.catalogo.domain.ports.out.CatalogoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAllProfessionalCategoriesUseCaseImplTest {

    @Mock
    private CatalogoRepository catalogoRepository;

    @InjectMocks
    private GetAllProfessionalCategoriesUseCaseImpl getAllProfessionalCategoriesUseCase;

    @Test
    void returnsTheCategoriesOfTheCatalogo() {
        List<ProfessionalCategory> categories =
                List.of(new ProfessionalCategory(1L, "Clinica medica", "Atencion clinica general"));
        when(catalogoRepository.findAllCategories()).thenReturn(categories);

        assertThat(getAllProfessionalCategoriesUseCase.getAllProfessionalCategories()).isSameAs(categories);
    }

    @Test
    void propagatesThatTheCatalogoIsUnavailable() {
        CatalogoUnavailableException unavailable = new CatalogoUnavailableException("down", null);
        when(catalogoRepository.findAllCategories()).thenThrow(unavailable);

        assertThatThrownBy(() -> getAllProfessionalCategoriesUseCase.getAllProfessionalCategories())
                .isSameAs(unavailable);
    }
}
