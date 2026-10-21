package com.toneatti.turnos.user.infrastructure.security;

import com.toneatti.turnos.config.SecurityConfig;
import com.toneatti.turnos.config.UserJwtProperties;
import com.toneatti.turnos.user.domain.model.AccessToken;
import com.toneatti.turnos.user.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAccessTokenIssuerAdapterTest {

    private static final String SECRET = "test-secret-with-at-least-32-bytes!";

    private final UserJwtProperties properties = new UserJwtProperties(SECRET, Duration.ofHours(2));
    private final JwtAccessTokenIssuerAdapter issuer =
            new JwtAccessTokenIssuerAdapter(new SecurityConfig().jwtEncoder(properties), properties);

    private final User user = new User(
            7L, "3f1c2b8e-0000-4000-8000-000000000001", "juan@example.com", "hashed-password",
            "Juan", "Perez", Instant.parse("2026-09-10T14:30:00Z"));

    @Test
    void issuesAnHs256TokenWhoseSubjectIsTheExternalId() {
        Jwt jwt = decode(issuer.issue(user).value());

        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getSubject()).isEqualTo("3f1c2b8e-0000-4000-8000-000000000001");
    }

    @Test
    void theTokenOnlyCarriesTheSubjectAndItsTimestamps() {
        Jwt jwt = decode(issuer.issue(user).value());

        assertThat(jwt.getClaims()).containsOnlyKeys("sub", "iat", "exp");
    }

    @Test
    void expiresAfterTheConfiguredLifetime() {
        AccessToken token = issuer.issue(user);
        Jwt jwt = decode(token.value());

        assertThat(Duration.between(token.issuedAt(), token.expiresAt())).isEqualTo(Duration.ofHours(2));
        assertThat(jwt.getIssuedAt()).isEqualTo(token.issuedAt());
        assertThat(jwt.getExpiresAt()).isEqualTo(token.expiresAt());
    }

    private static Jwt decode(String token) {
        SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build().decode(token);
    }
}
