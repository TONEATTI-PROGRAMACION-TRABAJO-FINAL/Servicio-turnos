package com.toneatti.turnos.catalogo.application.usecases;

import com.toneatti.turnos.catalogo.domain.model.Professional;
import com.toneatti.turnos.catalogo.domain.ports.in.GetProfessionalByIdUseCase;
import com.toneatti.turnos.catalogo.domain.ports.out.CatalogoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * A disabled professional is returned too: the app shows its name in past reservations and does not offer
 * its slots.
 */
@Component
@RequiredArgsConstructor
public class GetProfessionalByIdUseCaseImpl implements GetProfessionalByIdUseCase {

    private final CatalogoRepository catalogoRepository;

    @Override
    public Optional<Professional> getProfessionalById(Long id) {
        return catalogoRepository.findProfessionalById(id);
    }
}
