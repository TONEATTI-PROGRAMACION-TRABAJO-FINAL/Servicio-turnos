package com.toneatti.turnos.user.infrastructure.web.controller;

import com.toneatti.turnos.config.UserJwtProperties;
import com.toneatti.turnos.user.infrastructure.persistence.repository.JpaUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserLoginIntegrationTest {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JpaUserRepository jpaUserRepository;

    @Autowired
    private UserJwtProperties userJwtProperties;

    @Test
    void returnsABearerTokenForTheUserThatIsNeverCached() throws Exception {
        register("sofia.lopez@example.com");

        String body = mockMvc.perform(login("sofia.lopez@example.com", "secret-password"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86_400))
                .andReturn().getResponse().getContentAsString();

        String token = body.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");
        String externalId = jpaUserRepository.findByEmail("sofia.lopez@example.com").orElseThrow().getExternalId();
        assertThat(decoder().decode(token).getSubject()).isEqualTo(externalId);
    }

    @Test
    void acceptsTheEmailWithDifferentCase() throws Exception {
        register("tomas.vera@example.com");

        mockMvc.perform(login("Tomas.Vera@Example.COM", "secret-password"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsAWrongPasswordWithProblemJson() throws Exception {
        register("nora.paz@example.com");

        mockMvc.perform(login("nora.paz@example.com", "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value(INVALID_CREDENTIALS));
    }

    @Test
    void rejectsAnUnknownEmailWithTheSameProblemAsAWrongPassword() throws Exception {
        mockMvc.perform(login("nobody@example.com", "secret-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value(INVALID_CREDENTIALS));
    }

    @Test
    void rejectsAnInvalidBodyWithProblemJson() throws Exception {
        mockMvc.perform(login("not-an-email", ""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsAPasswordLongerThan128Characters() throws Exception {
        mockMvc.perform(login("someone@example.com", "ñ".repeat(129)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secret-password","firstName":"Test","lastName":"User"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    private static RequestBuilder login(String email, String password) {
        return post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password));
    }

    private NimbusJwtDecoder decoder() {
        SecretKeySpec key = new SecretKeySpec(
                userJwtProperties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
