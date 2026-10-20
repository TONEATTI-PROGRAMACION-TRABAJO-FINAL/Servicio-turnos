package com.toneatti.turnos.user.infrastructure.web.controller;

import com.toneatti.turnos.config.SecurityConfig;
import com.toneatti.turnos.config.UserJwtProperties;
import com.toneatti.turnos.user.infrastructure.persistence.repository.JpaUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CurrentUserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JpaUserRepository jpaUserRepository;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void returnsTheUserOfTheTokenWithoutSensitiveData() throws Exception {
        register("elena.rios@example.com");
        String externalId = jpaUserRepository.findByEmail("elena.rios@example.com").orElseThrow().getExternalId();

        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + login("elena.rios@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalId").value(externalId))
                .andExpect(jsonPath("$.email").value("elena.rios@example.com"))
                .andExpect(jsonPath("$.firstName").value("Elena"))
                .andExpect(jsonPath("$.lastName").value("Rios"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void answers404WithProblemJsonWhenTheSubjectDoesNotExist() throws Exception {
        String token = sign(jwtEncoder, UUID.randomUUID().toString(), Instant.now().plus(Duration.ofHours(1)));

        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void answers401WithoutAToken() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void answers401WithAnExpiredToken() throws Exception {
        register("pablo.sosa@example.com");
        String externalId = jpaUserRepository.findByEmail("pablo.sosa@example.com").orElseThrow().getExternalId();
        // Expired beyond the 60 second clock skew the decoder tolerates.
        String token = sign(jwtEncoder, externalId, Instant.now().minus(Duration.ofMinutes(5)));

        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void answers401WithATokenSignedWithAnotherSecret() throws Exception {
        register("ines.mora@example.com");
        String externalId = jpaUserRepository.findByEmail("ines.mora@example.com").orElseThrow().getExternalId();
        JwtEncoder otherEncoder = new SecurityConfig()
                .jwtEncoder(new UserJwtProperties("another-secret-with-at-least-32-bytes", Duration.ofHours(1)));
        String token = sign(otherEncoder, externalId, Instant.now().plus(Duration.ofHours(1)));

        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private void register(String email) throws Exception {
        String[] names = email.substring(0, email.indexOf('@')).split("\\.");
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secret-password","firstName":"%s","lastName":"%s"}
                                """.formatted(email, capitalize(names[0]), capitalize(names[1]))))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secret-password"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");
    }

    private static String sign(JwtEncoder encoder, String subject, Instant expiresAt) {
        Instant issuedAt = expiresAt.minus(Duration.ofHours(1)).truncatedTo(ChronoUnit.SECONDS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt.truncatedTo(ChronoUnit.SECONDS))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static String capitalize(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
