package com.toneatti.turnos.catalogo.application.service;

import com.toneatti.turnos.catalogo.domain.model.Professional;
import com.toneatti.turnos.catalogo.domain.model.ProfessionalCategory;
import com.toneatti.turnos.catalogo.domain.ports.in.GetAllProfessionalCategoriesUseCase;
import com.toneatti.turnos.catalogo.domain.ports.in.GetProfessionalsByCategoryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final GetAllProfessionalCategoriesUseCase getAllProfessionalCategoriesUseCase;
    private final GetProfessionalsByCategoryUseCase getProfessionalsByCategoryUseCase;

    public List<ProfessionalCategory> getAllProfessionalCategories() {
        return getAllProfessionalCategoriesUseCase.getAllProfessionalCategories();
    }

    public List<Professional> getProfessionalsByCategory(Long categoryId) {
        return getProfessionalsByCategoryUseCase.getProfessionalsByCategory(categoryId);
    }

}
