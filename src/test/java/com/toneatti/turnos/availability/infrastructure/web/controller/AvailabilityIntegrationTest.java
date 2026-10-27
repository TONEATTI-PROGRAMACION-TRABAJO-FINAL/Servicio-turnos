package com.toneatti.turnos.availability.infrastructure.web.controller;

import com.toneatti.turnos.support.FakeHttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

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
 * GET /available-slots against fake Servicio-catalogo and cátedra servers. The dates are in 2099 so no slot has
 * started yet; 2099-09-14 is a Monday.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AvailabilityIntegrationTest {

    // From application-test.properties.
    private static final String CATEDRA_JWT = "test-jwt";
    private static final String OCCUPANCIES_PATH = "/api/appointment-occupancies";
    private static final String WEEKLY_SCHEDULES_PATH = "/weekly-schedules";
    private static final String MONDAY_SCHEDULE = """
            [{"id":21,"professionalId":15,"dayOfWeek":"MONDAY","startTime":"09:00:00","endTime":"10:30:00",
              "slotDurationMinutes":30}]
            """;

    private static final FakeHttpServer CATALOGO = FakeHttpServer.start();
    private static final FakeHttpServer CATEDRA = FakeHttpServer.start();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void fakeServers(DynamicPropertyRegistry registry) {
        registry.add("catalogo.base-url", CATALOGO::baseUrl);
        registry.add("catedra.api-base-url", CATEDRA::baseUrl);
    }

    @AfterAll
    static void stopServers() {
        CATALOGO.close();
        CATEDRA.close();
    }

    @Test
    void returnsTheFreeSlotsOfTheProfessional() throws Exception {
        givenAnEnabledProfessional(15, MONDAY_SCHEDULE);
        CATEDRA.respond(OCCUPANCIES_PATH, 200, """
                {"professionalId":15,"from":"2099-09-14","to":"2099-09-14","occupancies":[
                  {"date":"2099-09-14","startTime":"09:30:00","status":"CONFIRMED","expiresAt":null}]}
                """);

        availableSlots("15", "2099-09-14", "2099-09-14")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].date").value("2099-09-14"))
                .andExpect(jsonPath("$[0].startTime").value("09:00"))
                .andExpect(jsonPath("$[0].endTime").value("09:30"))
                .andExpect(jsonPath("$[1].startTime").value("10:00"))
                .andExpect(content().string(not(containsString(CATEDRA_JWT))));
        assertThat(CATEDRA.lastRequestUri().getQuery()).isEqualTo("professionalId=15&from=2099-09-14&to=2099-09-14");
        assertThat(CATEDRA.lastRequestHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + CATEDRA_JWT);
    }

    @Test
    void returnsAnEmptyListWhenThereAreNoSlots() throws Exception {
        givenAnEnabledProfessional(16, "[]");

        availableSlots("16", "2099-09-14", "2099-09-20")
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void answers401WithoutAUserToken() throws Exception {
        mockMvc.perform(get("/available-slots")
                        .param("professionalId", "15").param("from", "2099-09-14").param("to", "2099-09-14"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void answers400WithInvalidDateRangeWhenFromIsAfterTo() throws Exception {
        expectProblem(availableSlots("15", "2099-09-15", "2099-09-14"), 400, "INVALID_DATE_RANGE");
    }

    @Test
    void answers400WithInvalidDateRangeWhenTheCatedraRejectsTheRange() throws Exception {
        givenAnEnabledProfessional(17, MONDAY_SCHEDULE.replace("15", "17"));
        CATEDRA.respondProblem(OCCUPANCIES_PATH, 400, "INVALID_DATE_RANGE");

        expectProblem(availableSlots("17", "2099-09-14", "2099-09-14"), 400, "INVALID_DATE_RANGE");
    }

    @Test
    void answers400WhenTheRangeIsLongerThan31Days() throws Exception {
        expectProblem(availableSlots("15", "2099-09-01", "2099-10-02"), 400, "DATE_RANGE_TOO_LONG");
    }

    @Test
    void answers400WhenADateIsNotInTheContractFormat() throws Exception {
        availableSlots("15", "14-09-2099", "2099-09-14")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void answers400WhenAParameterIsMissing() throws Exception {
        mockMvc.perform(get("/available-slots").with(jwt())
                        .param("from", "2099-09-14").param("to", "2099-09-14"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void answers404WhenTheProfessionalDoesNotExist() throws Exception {
        // The fake catalogo answers 404 for every path without a canned response.
        expectProblem(availableSlots("99", "2099-09-14", "2099-09-14"), 404, "PROFESSIONAL_NOT_FOUND");
    }

    @Test
    void answers400WhenTheProfessionalIsDisabled() throws Exception {
        CATALOGO.respond("/professionals/18", 200, """
                {"id":18,"categoryId":1,"firstName":"Luis","lastName":"Diaz","enabled":false}
                """);

        expectProblem(availableSlots("18", "2099-09-14", "2099-09-14"), 400, "PROFESSIONAL_DISABLED");
    }

    @Test
    void answers503WhenTheCatalogoIsDown() throws Exception {
        CATALOGO.respond("/professionals/19", 500, "{\"status\":500,\"title\":\"Internal Server Error\"}");

        expectProblem(availableSlots("19", "2099-09-14", "2099-09-14"), 503, "SERVICE_UNAVAILABLE");
    }

    @Test
    void answers503WithoutDetailsOfTheCatedraWhenItIsDown() throws Exception {
        givenAnEnabledProfessional(20, MONDAY_SCHEDULE.replace("15", "20"));
        CATEDRA.respond(OCCUPANCIES_PATH, 503, "{\"status\":503,\"detail\":\"redis down\"}");

        expectProblem(availableSlots("20", "2099-09-14", "2099-09-14"), 503, "SERVICE_UNAVAILABLE")
                .andExpect(content().string(not(containsString("redis down"))))
                .andExpect(content().string(not(containsString(CATEDRA_JWT))));
    }

    private ResultActions availableSlots(String professionalId, String from, String to) throws Exception {
        return mockMvc.perform(get("/available-slots").with(jwt())
                .param("professionalId", professionalId).param("from", from).param("to", to));
    }

    private static void givenAnEnabledProfessional(int id, String weeklySchedules) {
        CATALOGO.respond("/professionals/" + id, 200, """
                {"id":%d,"categoryId":1,"firstName":"Ana","lastName":"Gomez","enabled":true}
                """.formatted(id));
        CATALOGO.respond(WEEKLY_SCHEDULES_PATH, 200, weeklySchedules);
    }
}
