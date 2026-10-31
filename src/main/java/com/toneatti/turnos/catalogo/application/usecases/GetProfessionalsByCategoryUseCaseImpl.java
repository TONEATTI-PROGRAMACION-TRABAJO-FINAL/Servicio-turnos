package com.toneatti.turnos.catalogo.application.usecases;

import com.toneatti.turnos.catalogo.domain.model.Professional;
import com.toneatti.turnos.catalogo.domain.ports.in.GetProfessionalsByCategoryUseCase;
import com.toneatti.turnos.catalogo.domain.ports.out.CatalogoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Servicio-catalogo already lists only the enabled professionals; filtering again keeps a disabled one from being
 * offered if that ever changes. Excluding the professionals of a disabled category is up to Servicio-catalogo.
 */
@Component
@RequiredArgsConstructor
public class GetProfessionalsByCategoryUseCaseImpl implements GetProfessionalsByCategoryUseCase {

    private final CatalogoRepository catalogoRepository;

    @Override
    public List<Professional> getProfessionalsByCategory(Long categoryId) {
        return catalogoRepository.findProfessionalsByCategoryId(categoryId).stream()
                .filter(Professional::enabled)
                .toList();
    }
}
