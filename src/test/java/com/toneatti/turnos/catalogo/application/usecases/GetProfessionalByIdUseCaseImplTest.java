package com.toneatti.turnos.catalogo.application.usecases;

import com.toneatti.turnos.catalogo.domain.exception.CatalogoUnavailableException;
import com.toneatti.turnos.catalogo.domain.model.Professional;
import com.toneatti.turnos.catalogo.domain.ports.out.CatalogoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProfessionalByIdUseCaseImplTest {

    @Mock
    private CatalogoRepository catalogoRepository;

    @InjectMocks
    private GetProfessionalByIdUseCaseImpl getProfessionalByIdUseCase;

    @Test
    void returnsADisabledProfessionalToo() {
        Professional luis = new Professional(18L, 1L, "Luis", "Diaz", false);
        when(catalogoRepository.findProfessionalById(18L)).thenReturn(Optional.of(luis));

        assertThat(getProfessionalByIdUseCase.getProfessionalById(18L)).contains(luis);
    }

    @Test
    void returnsEmptyWhenTheProfessionalDoesNotExist() {
        when(catalogoRepository.findProfessionalById(99L)).thenReturn(Optional.empty());

        assertThat(getProfessionalByIdUseCase.getProfessionalById(99L)).isEmpty();
    }

    @Test
    void propagatesThatTheCatalogoIsUnavailable() {
        CatalogoUnavailableException unavailable = new CatalogoUnavailableException("down", null);
        when(catalogoRepository.findProfessionalById(15L)).thenThrow(unavailable);

        assertThatThrownBy(() -> getProfessionalByIdUseCase.getProfessionalById(15L)).isSameAs(unavailable);
    }
}
