package com.toneatti.turnos.catalogo.domain.ports.in;

import com.toneatti.turnos.catalogo.domain.model.Professional;

import java.util.Optional;

public interface GetProfessionalByIdUseCase {
    /**
     * The professional with that id, enabled or not; empty when it does not exist.
     */
    Optional<Professional> getProfessionalById(Long id);
}
