package com.toneatti.turnos.catalogo.domain.ports.in;

import com.toneatti.turnos.catalogo.domain.model.Professional;

import java.util.List;

public interface GetProfessionalsByCategoryUseCase {
    /**
     * The enabled professionals of the category, or of every category when {@code categoryId} is null.
     */
    List<Professional> getProfessionalsByCategory(Long categoryId);
}
