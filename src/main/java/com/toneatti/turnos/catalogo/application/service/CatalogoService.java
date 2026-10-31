package com.toneatti.turnos.catalogo.application.service;

import com.toneatti.turnos.catalogo.application.exception.CatalogoException;
import com.toneatti.turnos.catalogo.domain.model.Professional;
import com.toneatti.turnos.catalogo.domain.model.ProfessionalCategory;
import com.toneatti.turnos.catalogo.domain.ports.in.GetAllProfessionalCategoriesUseCase;
import com.toneatti.turnos.catalogo.domain.ports.in.GetProfessionalByIdUseCase;
import com.toneatti.turnos.catalogo.domain.ports.in.GetProfessionalsByCategoryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final GetAllProfessionalCategoriesUseCase getAllProfessionalCategoriesUseCase;
    private final GetProfessionalsByCategoryUseCase getProfessionalsByCategoryUseCase;
    private final GetProfessionalByIdUseCase getProfessionalByIdUseCase;

    public List<ProfessionalCategory> getAllProfessionalCategories() {
        return getAllProfessionalCategoriesUseCase.getAllProfessionalCategories();
    }

    public List<Professional> getProfessionalsByCategory(Long categoryId) {
        return getProfessionalsByCategoryUseCase.getProfessionalsByCategory(categoryId);
    }

    public Professional getProfessionalById(Long id) {
        return getProfessionalByIdUseCase.getProfessionalById(id)
                .orElseThrow(() -> new CatalogoException("Could not find professional with id: " + id));
    }

}
