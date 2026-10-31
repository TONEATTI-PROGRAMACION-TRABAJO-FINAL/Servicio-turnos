package com.toneatti.turnos.catalogo.infrastructure.web.controller;

import com.toneatti.turnos.support.FakeHttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.toneatti.turnos.support.ProblemAssertions.expectProblem;
import static org.assertj.core.api.Assertions.assertThat;
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
    private static final String PROFESSIONALS_PATH = "/professionals";
    private static final String PROFESSIONAL_15_PATH = "/professionals/15";

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

    @ParameterizedTest
    @ValueSource(strings = {CATEGORIES_PATH, PROFESSIONALS_PATH, PROFESSIONAL_15_PATH})
    void answers401WithoutAUserToken(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {CATEGORIES_PATH, PROFESSIONALS_PATH, PROFESSIONAL_15_PATH})
    void answers503WithoutDetailsOfTheCatalogoWhenItIsDown(String path) throws Exception {
        CATALOGO.respond(path, 500, "{\"status\":500,\"detail\":\"database down\"}");

        expectProblem(mockMvc.perform(get(path).with(jwt())), 503, "SERVICE_UNAVAILABLE")
                .andExpect(content().string(not(containsString("database down"))));
    }

    @Test
    void listsOnlyTheEnabledProfessionalsOfTheCategory() throws Exception {
        CATALOGO.respond(PROFESSIONALS_PATH, 200, """
                [{"id":15,"categoryId":1,"firstName":"Ana","lastName":"Gomez","enabled":true},
                 {"id":18,"categoryId":1,"firstName":"Luis","lastName":"Diaz","enabled":false}]
                """);

        mockMvc.perform(get(PROFESSIONALS_PATH).with(jwt()).param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(15))
                .andExpect(jsonPath("$[0].categoryId").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Ana"))
                .andExpect(jsonPath("$[0].lastName").value("Gomez"))
                .andExpect(jsonPath("$[0].enabled").value(true));
        assertThat(CATALOGO.lastRequestUri().getQuery()).isEqualTo("categoryId=1");
    }

    @Test
    void listsTheProfessionalsOfEveryCategoryWithoutACategory() throws Exception {
        CATALOGO.respond(PROFESSIONALS_PATH, 200, """
                [{"id":15,"categoryId":1,"firstName":"Ana","lastName":"Gomez","enabled":true},
                 {"id":20,"categoryId":2,"firstName":"Eva","lastName":"Ruiz","enabled":true}]
                """);

        mockMvc.perform(get(PROFESSIONALS_PATH).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        assertThat(CATALOGO.lastRequestUri().getQuery()).isNull();
    }

    @Test
    void answers400WhenTheCategoryIsNotANumber() throws Exception {
        mockMvc.perform(get(PROFESSIONALS_PATH).with(jwt()).param("categoryId", "uno"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void showsAnEnabledProfessional() throws Exception {
        CATALOGO.respond(PROFESSIONAL_15_PATH, 200, """
                {"id":15,"categoryId":1,"firstName":"Ana","lastName":"Gomez","enabled":true}
                """);

        mockMvc.perform(get(PROFESSIONAL_15_PATH).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.lastName").value("Gomez"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void showsADisabledProfessionalWithEnabledFalse() throws Exception {
        CATALOGO.respond("/professionals/18", 200, """
                {"id":18,"categoryId":1,"firstName":"Luis","lastName":"Diaz","enabled":false}
                """);

        mockMvc.perform(get("/professionals/18").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(18))
                .andExpect(jsonPath("$.firstName").value("Luis"))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void answers404WhenTheProfessionalDoesNotExist() throws Exception {
        CATALOGO.respond("/professionals/99", 404, "{\"status\":404,\"title\":\"Not Found\"}");

        expectProblem(mockMvc.perform(get("/professionals/99").with(jwt())), 404, "PROFESSIONAL_NOT_FOUND");
    }

    @Test
    void answers400WhenTheProfessionalIdIsNotANumber() throws Exception {
        mockMvc.perform(get("/professionals/quince").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
