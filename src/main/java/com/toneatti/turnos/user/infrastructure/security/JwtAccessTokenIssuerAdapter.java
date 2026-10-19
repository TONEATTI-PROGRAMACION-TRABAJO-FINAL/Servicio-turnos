package com.toneatti.turnos.user.infrastructure.security;

import com.toneatti.turnos.config.UserJwtProperties;
import com.toneatti.turnos.user.domain.model.AccessToken;
import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.domain.ports.out.AccessTokenIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Signs the user JWT with the {@link JwtEncoder} defined in {@code config.SecurityConfig}.
 * The subject is the externalId: it never changes and does not expose the database id or the email.
 */
@Component
@RequiredArgsConstructor
public class JwtAccessTokenIssuerAdapter implements AccessTokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final UserJwtProperties userJwtProperties;

    @Override
    public AccessToken issue(User user) {
        // JWT timestamps have second precision.
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(userJwtProperties.expiration());
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.externalId())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, issuedAt, expiresAt);
    }
}
