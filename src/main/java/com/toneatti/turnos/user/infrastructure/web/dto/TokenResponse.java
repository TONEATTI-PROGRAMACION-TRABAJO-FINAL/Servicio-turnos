package com.toneatti.turnos.user.infrastructure.web.dto;

import lombok.*;

// Same shape as an OAuth2 token response (RFC 6749, section 5.1), in camelCase like the rest of the API.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
}
