package com.toneatti.turnos.catedra.infrastructure.rest.dto;

/**
 * The part of an application/problem+json error of the cátedra that callers may rely on: {@code status} and
 * {@code code}. The other fields, {@code detail} included, are ignored.
 */
public record CatedraProblemResponse(
        Integer status,
        String code
) {
}
