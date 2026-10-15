package com.toneatti.turnos.config;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Base URL of our own Servicio-catalogo, consumed by this service. Comes from an environment variable.
 */
@Validated
@ConfigurationProperties(prefix = "catalogo")
public record CatalogoServiceProperties(
        @NotBlank @URL String baseUrl) {
}
