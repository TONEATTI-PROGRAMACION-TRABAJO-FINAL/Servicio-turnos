package com.toneatti.turnos.catalogo.application.usecases;

import com.toneatti.turnos.catalogo.domain.model.ProfessionalCategory;
import com.toneatti.turnos.catalogo.domain.ports.in.GetAllProfessionalCategoriesUseCase;
import com.toneatti.turnos.catalogo.domain.ports.out.CatalogoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The category model has no enabled flag: excluding the disabled ones is up to Servicio-catalogo.
 */
@Component
@RequiredArgsConstructor
public class GetAllProfessionalCategoriesUseCaseImpl implements GetAllProfessionalCategoriesUseCase {

    private final CatalogoRepository catalogoRepository;

    @Override
    public List<ProfessionalCategory> getAllProfessionalCategories() {
        return catalogoRepository.findAllCategories();
    }
}
