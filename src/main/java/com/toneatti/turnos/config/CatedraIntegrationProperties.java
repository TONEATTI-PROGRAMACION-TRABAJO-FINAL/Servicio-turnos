package com.toneatti.turnos.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Connection data for the cátedra service (sections 3, 5 and 15 of the integration contract).
 * Values come from environment variables; the JWT is masked in toString so it never reaches the logs.
 */
@Validated
@ConfigurationProperties(prefix = "catedra")
public record CatedraIntegrationProperties(
        @NotBlank @URL String apiBaseUrl,
        @NotBlank String jwt,
        @NotNull @Valid Kafka kafka) {

    private static final String MASK = "****";

    @Override
    public String toString() {
        return "CatedraIntegrationProperties[apiBaseUrl=" + apiBaseUrl + ", jwt=" + MASK
                + ", kafka=" + kafka + "]";
    }

    public record Kafka(
            @NotBlank String bootstrapServers,
            @NotBlank String consumerGroupId,
            @NotBlank String appointmentActionsTopic,
            @NotBlank String appointmentPhoneTopic) {
    }
}
