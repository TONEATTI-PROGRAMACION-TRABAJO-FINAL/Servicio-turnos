package com.toneatti.turnos.catalogo.domain.model;

public record Professional(
        Long id,
        Long categoryId,
        String firstName,
        String lastName,
        boolean enabled
) {
}
