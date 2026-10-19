package com.toneatti.turnos.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Signing secret and lifetime of the JWT issued to the end users of the app (HS256).
 * The secret comes from an environment variable and is masked in toString so it never reaches the logs.
 */
@Validated
@ConfigurationProperties(prefix = "user.jwt")
public record UserJwtProperties(
        @NotBlank String secret,
        @NotNull @DefaultValue("PT24H") Duration expiration) {

    // HS256 needs a key of at least 256 bits.
    private static final int MIN_SECRET_BYTES = 32;
    private static final String MASK = "****";

    @AssertTrue(message = "must have at least 32 bytes in UTF-8")
    public boolean isSecretLongEnough() {
        // A missing secret is reported by @NotBlank only.
        return secret == null || secret.isBlank()
                || secret.getBytes(StandardCharsets.UTF_8).length >= MIN_SECRET_BYTES;
    }

    @AssertTrue(message = "must be positive")
    public boolean isExpirationPositive() {
        return expiration == null || expiration.isPositive();
    }

    @Override
    public String toString() {
        return "UserJwtProperties[secret=" + MASK + ", expiration=" + expiration + "]";
    }
}
