package com.toneatti.turnos.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;
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
        @NotNull @DefaultValue("PT2S") Duration connectTimeout,
        @NotNull @DefaultValue("PT3S") Duration readTimeout) {

    @AssertTrue(message = "must be positive")
    public boolean isConnectTimeoutPositive() {
        return connectTimeout == null || connectTimeout.isPositive();
    }

    @AssertTrue(message = "must be positive")
    public boolean isReadTimeoutPositive() {
        return readTimeout == null || readTimeout.isPositive();
    }
}
