package com.toneatti.turnos.catalogo.infrastructure.web.controller;

import com.toneatti.turnos.support.FakeHttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.toneatti.turnos.support.ProblemAssertions.expectProblem;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The catalog endpoints for the app against a fake Servicio-catalogo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogoIntegrationTest {

    private static final String CATEGORIES_PATH = "/professional-categories";

    private static final FakeHttpServer CATALOGO = FakeHttpServer.start();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void fakeServers(DynamicPropertyRegistry registry) {
        registry.add("catalogo.base-url", CATALOGO::baseUrl);
    }

    @AfterAll
    static void stopServers() {
        CATALOGO.close();
    }

    @Test
    void listsTheCategoriesOfTheCatalogo() throws Exception {
        CATALOGO.respond(CATEGORIES_PATH, 200, """
                [{"id":1,"name":"Clinica medica","description":"Atencion clinica general"},
                 {"id":2,"name":"Pediatria","description":"Atencion de ninos"}]
                """);

        mockMvc.perform(get(CATEGORIES_PATH).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Clinica medica"))
                .andExpect(jsonPath("$[0].description").value("Atencion clinica general"))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void listsNoCategoriesWhenTheCatalogoHasNone() throws Exception {
        CATALOGO.respond(CATEGORIES_PATH, 200, "[]");

        mockMvc.perform(get(CATEGORIES_PATH).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void answers401WithoutAUserToken() throws Exception {
        mockMvc.perform(get(CATEGORIES_PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    void answers503WithoutDetailsOfTheCatalogoWhenItIsDown() throws Exception {
        CATALOGO.respond(CATEGORIES_PATH, 500, "{\"status\":500,\"detail\":\"database down\"}");

        expectProblem(mockMvc.perform(get(CATEGORIES_PATH).with(jwt())), 503, "SERVICE_UNAVAILABLE")
                .andExpect(content().string(not(containsString("database down"))));
    }
}
