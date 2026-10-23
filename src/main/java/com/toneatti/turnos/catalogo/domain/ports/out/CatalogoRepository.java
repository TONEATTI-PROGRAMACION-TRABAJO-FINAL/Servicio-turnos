package com.toneatti.turnos.catalogo.domain.ports.out;

import com.toneatti.turnos.catalogo.domain.model.Professional;
import com.toneatti.turnos.catalogo.domain.model.ProfessionalCategory;

import java.util.List;
import java.util.Optional;

/**
 * Read-only access to the local copy of the catalog kept by Servicio-catalogo.
 */
public interface CatalogoRepository {

    List<ProfessionalCategory> findAllCategories();

    /**
     * Enabled professionals of the given category, or of every category when {@code categoryId} is null.
     */
    List<Professional> findProfessionalsByCategoryId(Long categoryId);

    /**
     * The professional with that id, enabled or not, so past reservations can still show the name.
     */
    Optional<Professional> findProfessionalById(Long id);

}
