package com.toneatti.turnos.catalogo.infrastructure.rest.dto;

public record CatalogoProfessionalResponse(
        Long id,
        Long categoryId,
        String firstName,
        String lastName,
        boolean enabled
) {
}
