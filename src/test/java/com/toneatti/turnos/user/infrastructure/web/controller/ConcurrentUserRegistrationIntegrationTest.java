package com.toneatti.turnos.user.infrastructure.web.controller;

import com.toneatti.turnos.user.infrastructure.persistence.repository.JpaUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Simulates two concurrent registrations: the second one passes the existsByEmail check before
 * the first one is visible, so only the unique constraint of the database can stop it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConcurrentUserRegistrationIntegrationTest {

    private static final String EMAIL = "carla.ruiz@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private JpaUserRepository jpaUserRepository;

    @Test
    void aUniqueViolationOnTheEmailRespondsConflictWithProblemJson() throws Exception {
        String body = """
                {"email":"%s","password":"secret-password","firstName":"Carla","lastName":"Ruiz"}
                """.formatted(EMAIL);
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        // The use case check sees the email as free and the adapter's check after the violation sees it taken.
        // The insert is real, so the unique violation comes from the database.
        doReturn(false).doReturn(true).when(jpaUserRepository).existsByEmail(EMAIL);

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Email already registered"));
    }
}
