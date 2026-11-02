package com.toneatti.turnos.catalogo.application.usecases;

import com.toneatti.turnos.catalogo.domain.exception.CatalogoUnavailableException;
import com.toneatti.turnos.catalogo.domain.model.Professional;
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
class GetProfessionalsByCategoryUseCaseImplTest {

    private static final Professional ANA = new Professional(15L, 1L, "Ana", "Gomez", true);
    private static final Professional LUIS = new Professional(18L, 1L, "Luis", "Diaz", false);
    private static final Professional EVA = new Professional(20L, 2L, "Eva", "Ruiz", true);

    @Mock
    private CatalogoRepository catalogoRepository;

    @InjectMocks
    private GetProfessionalsByCategoryUseCaseImpl getProfessionalsByCategoryUseCase;

    @Test
    void returnsTheEnabledProfessionalsOfTheCategory() {
        when(catalogoRepository.findProfessionalsByCategoryId(1L)).thenReturn(List.of(ANA, LUIS));

        assertThat(getProfessionalsByCategoryUseCase.getProfessionalsByCategory(1L)).containsExactly(ANA);
    }

    @Test
    void returnsTheEnabledProfessionalsOfEveryCategoryWithoutACategory() {
        when(catalogoRepository.findProfessionalsByCategoryId(null)).thenReturn(List.of(ANA, LUIS, EVA));

        assertThat(getProfessionalsByCategoryUseCase.getProfessionalsByCategory(null)).containsExactly(ANA, EVA);
    }

    @Test
    void propagatesThatTheCatalogoIsUnavailable() {
        CatalogoUnavailableException unavailable = new CatalogoUnavailableException("down", null);
        when(catalogoRepository.findProfessionalsByCategoryId(1L)).thenThrow(unavailable);

        assertThatThrownBy(() -> getProfessionalsByCategoryUseCase.getProfessionalsByCategory(1L))
                .isSameAs(unavailable);
    }
}
