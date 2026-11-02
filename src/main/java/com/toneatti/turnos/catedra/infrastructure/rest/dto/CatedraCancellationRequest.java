package com.toneatti.turnos.catedra.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * {@code reason} is optional (section 12): without it the body is {@code {}}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CatedraCancellationRequest(
        String reason
) {
}
