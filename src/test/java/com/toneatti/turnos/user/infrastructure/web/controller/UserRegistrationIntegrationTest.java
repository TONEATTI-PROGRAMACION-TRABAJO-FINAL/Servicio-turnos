package com.toneatti.turnos.user.infrastructure.web.controller;

import com.toneatti.turnos.user.infrastructure.persistence.entity.UserEntity;
import com.toneatti.turnos.user.infrastructure.persistence.repository.JpaUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JpaUserRepository jpaUserRepository;

    @Test
    void registersAUserWithoutAuthenticationAndNeverReturnsThePassword() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ana.gomez@example.com", "secret-password", "Ana", "Gomez")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalId").isNotEmpty())
                .andExpect(jsonPath("$.email").value("ana.gomez@example.com"))
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.lastName").value("Gomez"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());

        UserEntity stored = jpaUserRepository.findAll().stream()
                .filter(user -> user.getEmail().equals("ana.gomez@example.com"))
                .findFirst()
                .orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo("secret-password").startsWith("$2");
    }

    @Test
    void rejectsADuplicateEmailWithProblemJson() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("luis.diaz@example.com", "secret-password", "Luis", "Diaz")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Luis.Diaz@Example.com", "other-password", "Luis", "Diaz")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Email already registered"));
    }

    @Test
    void rejectsAnInvalidBodyWithProblemJson() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("not-an-email", "short", "A", "")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void acceptsAPasswordOfExactly72BytesInUtf8() throws Exception {
        // "ñ" takes 2 bytes in UTF-8: 36 characters are 72 bytes.
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("marta.nunez@example.com", "ñ".repeat(36), "Marta", "Nuñez")))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsAPasswordLongerThan72BytesInUtf8WithProblemJson() throws Exception {
        // 37 characters, under the old 72 character limit, but 74 bytes in UTF-8.
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("pedro.nunez@example.com", "ñ".repeat(37), "Pedro", "Nuñez")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));

        assertThat(jpaUserRepository.existsByEmail("pedro.nunez@example.com")).isFalse();
    }

    @Test
    void otherEndpointsAreNotPublic() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    private static String body(String email, String password, String firstName, String lastName) {
        return """
                {"email":"%s","password":"%s","firstName":"%s","lastName":"%s"}
                """.formatted(email, password, firstName, lastName);
    }
}
