package com.toneatti.turnos.catalogo.domain.ports.out;

import com.toneatti.turnos.catalogo.domain.model.ProfessionalCategory;

import java.util.List;

/**
 * Read-only access to the local copy of the catalog kept by Servicio-catalogo.
 */
public interface CatalogoRepository {

    List<ProfessionalCategory> findAllCategories();

}
