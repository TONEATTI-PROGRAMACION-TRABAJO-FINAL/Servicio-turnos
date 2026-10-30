package com.toneatti.turnos.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Connection data for the cátedra service (sections 3, 5 and 15 of the integration contract) and the timeouts of its
 * REST client. {@code groupId} identifies the technical integration (section 2.1) in the events sent to the cátedra.
 * The contract gives Kafka no credentials, so its security protocol defaults to PLAINTEXT.
 * Values come from environment variables; the JWT is masked in toString so it never reaches the logs.
 */
@Validated
@ConfigurationProperties(prefix = "catedra")
public record CatedraIntegrationProperties(
        @NotBlank @URL String apiBaseUrl,
        @NotBlank String jwt,
        @NotBlank String groupId,
        @NotNull @DurationMin(millis = 1) @DefaultValue("PT2S") Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("PT5S") Duration readTimeout,
        @NotNull @Valid Kafka kafka) {

    private static final String MASK = "****";

    @Override
    public String toString() {
        return "CatedraIntegrationProperties[apiBaseUrl=" + apiBaseUrl + ", jwt=" + MASK + ", groupId=" + groupId
                + ", connectTimeout=" + connectTimeout + ", readTimeout=" + readTimeout + ", kafka=" + kafka + "]";
    }

    /**
     * {@code autoOffsetReset} is where a consumer group without committed offsets starts: earliest, so the events
     * published before the first start are not lost. {@code retryMaxElapsedTime} bounds the retries of an event whose
     * processing fails. {@code sendTimeout} bounds how long publishing an event waits for the broker, including
     * fetching its metadata. {@code listenerEnabled} false leaves the consumer stopped, for tests without a broker.
     */
    public record Kafka(
            @NotBlank String bootstrapServers,
            @NotBlank String consumerGroupId,
            @NotBlank String appointmentActionsTopic,
            @NotBlank String appointmentPhoneTopic,
            @NotNull @Pattern(regexp = "PLAINTEXT|SSL|SASL_PLAINTEXT|SASL_SSL") @DefaultValue("PLAINTEXT")
            String securityProtocol,
            @NotNull @Pattern(regexp = "earliest|latest") @DefaultValue("earliest") String autoOffsetReset,
            @NotNull @DurationMin(millis = 1) @DefaultValue("PT2M") Duration retryMaxElapsedTime,
            @NotNull @DurationMin(millis = 1) @DefaultValue("PT5S") Duration sendTimeout,
            @DefaultValue("true") boolean listenerEnabled) {
    }
}
