package com.toneatti.turnos.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Connection data for the cátedra service (sections 3, 5 and 15 of the integration contract) and the timeouts of its
 * REST client.
 * Values come from environment variables; the JWT is masked in toString so it never reaches the logs.
 */
@Validated
@ConfigurationProperties(prefix = "catedra")
public record CatedraIntegrationProperties(
        @NotBlank @URL String apiBaseUrl,
        @NotBlank String jwt,
        @NotNull @DurationMin(millis = 1) @DefaultValue("PT2S") Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("PT5S") Duration readTimeout,
        @NotNull @Valid Kafka kafka) {

    private static final String MASK = "****";

    @Override
    public String toString() {
        return "CatedraIntegrationProperties[apiBaseUrl=" + apiBaseUrl + ", jwt=" + MASK
                + ", connectTimeout=" + connectTimeout + ", readTimeout=" + readTimeout + ", kafka=" + kafka + "]";
    }

    public record Kafka(
            @NotBlank String bootstrapServers,
            @NotBlank String consumerGroupId,
            @NotBlank String appointmentActionsTopic,
            @NotBlank String appointmentPhoneTopic) {
    }
}
