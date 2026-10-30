package com.toneatti.turnos.catalogo.domain.ports.in;

import com.toneatti.turnos.catalogo.domain.model.ProfessionalCategory;

import java.util.List;

public interface GetAllProfessionalCategoriesUseCase {
    /**
     * The enabled categories, as Servicio-catalogo lists them.
     */
    List<ProfessionalCategory> getAllProfessionalCategories();
}
