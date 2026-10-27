package com.toneatti.turnos.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Base URL and timeouts of our own Servicio-catalogo, consumed by this service. Come from environment variables.
 */
@Validated
@ConfigurationProperties(prefix = "catalogo")
public record CatalogoServiceProperties(
        @NotBlank @URL String baseUrl,
        @NotNull @DurationMin(millis = 1) @DefaultValue("PT2S") Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("PT3S") Duration readTimeout) {
}
