package com.toneatti.turnos.reservation.infrastructure.web.controller;

import com.toneatti.turnos.reservation.domain.model.ReservationStatus;
import com.toneatti.turnos.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import com.toneatti.turnos.reservation.infrastructure.persistence.repository.JpaReservationProcessRepository;
import com.toneatti.turnos.support.FakeHttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static com.toneatti.turnos.support.ProblemAssertions.expectProblem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /reservations/{reservationProcessId}/cancel against a fake cátedra. Each test stores its own process with its
 * own reservationProcessId, so the path of the cátedra is its own too.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CancellationIntegrationTest {

    private static final Duration READ_TIMEOUT = Duration.ofMillis(500);
    // Longer than the read timeout: the request reaches the cátedra but its answer is lost.
    private static final Duration TIMEOUT = READ_TIMEOUT.multipliedBy(4);
    private static final String APPOINTMENTS_PATH = "/api/appointments";
    private static final String REASON = """
            {"reason":"Cambio solicitado por el usuario"}""";

    private static final FakeHttpServer CATEDRA = FakeHttpServer.start();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JpaReservationProcessRepository jpaReservationProcessRepository;

    private String userExternalId;
    private String processId;
    private String cancelPath;

    @DynamicPropertySource
    static void fakeCatedra(DynamicPropertyRegistry registry) {
        registry.add("catedra.api-base-url", CATEDRA::baseUrl);
        registry.add("catedra.read-timeout", READ_TIMEOUT::toString);
    }

    @AfterAll
    static void stopCatedra() {
        CATEDRA.close();
    }

    @BeforeEach
    void identifyTheTest() {
        CATEDRA.reset();
        userExternalId = UUID.randomUUID().toString();
        processId = "process-" + UUID.randomUUID();
        cancelPath = APPOINTMENTS_PATH + "/" + processId + "/cancel";
    }

    // The in-memory database is shared with the other tests of the JVM.
    @AfterEach
    void deleteData() {
        jpaReservationProcessRepository.deleteAll();
    }

    @Test
    void cancelsAConfirmedReservationOfTheUser() throws Exception {
        Long id = givenAProcess(ReservationStatus.CONFIRMED);
        CATEDRA.respond(cancelPath, 200, cancelled());

        expectCancelled(cancel(processId, userExternalId, REASON), id);

        assertThat(CATEDRA.lastRequestBody()).isEqualTo(REASON);
        assertStoredStatus(id, ReservationStatus.CANCELLED);
    }

    @Test
    void cancelsWithoutABody() throws Exception {
        Long id = givenAProcess(ReservationStatus.CONFIRMED);
        CATEDRA.respond(cancelPath, 200, cancelled());

        mockMvc.perform(withUser(post("/reservations/" + processId + "/cancel"), userExternalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancellationReason").isEmpty());

        assertThat(CATEDRA.lastRequestBody()).isEqualTo("{}");
        assertStoredStatus(id, ReservationStatus.CANCELLED);
    }

    // Section 12: cancelling again is idempotent. The second request finds it cancelled and does not call the cátedra.
    @Test
    void answersTheSameWhenTheReservationIsCancelledAgain() throws Exception {
        Long id = givenAProcess(ReservationStatus.CONFIRMED);
        CATEDRA.respond(cancelPath, 200, cancelled());

        cancel(processId, userExternalId, REASON).andExpect(status().isOk());
        expectCancelled(cancel(processId, userExternalId, "{\"reason\":\"Otro motivo\"}"), id);

        assertThat(CATEDRA.requestCount(cancelPath)).isEqualTo(1);
    }

    @Test
    void answers404WithoutCallingTheCatedraForAReservationOfAnotherUser() throws Exception {
        Long id = givenAProcess(ReservationStatus.CONFIRMED);

        expectProblem(cancel(processId, UUID.randomUUID().toString(), REASON), 404, "RESERVATION_NOT_FOUND");

        assertThat(CATEDRA.requestCount(cancelPath)).isZero();
        assertStoredStatus(id, ReservationStatus.CONFIRMED);
    }

    @Test
    void answers404ForAReservationThatDoesNotExist() throws Exception {
        expectProblem(cancel(processId, userExternalId, REASON), 404, "RESERVATION_NOT_FOUND");

        assertThat(CATEDRA.requestCount(cancelPath)).isZero();
    }

    @Test
    void answers409WithoutCallingTheCatedraWhenTheReservationIsNotConfirmedYet() throws Exception {
        Long id = givenAProcess(ReservationStatus.WAITING_FOR_PHONE);

        expectProblem(cancel(processId, userExternalId, REASON), 409, "RESERVATION_NOT_CANCELLABLE");

        assertThat(CATEDRA.requestCount(cancelPath)).isZero();
        assertStoredStatus(id, ReservationStatus.WAITING_FOR_PHONE);
    }

    @Test
    void answers400WithoutCallingTheCatedraWhenTheReasonIsTooLong() throws Exception {
        givenAProcess(ReservationStatus.CONFIRMED);

        expectProblem(cancel(processId, userExternalId, "{\"reason\":\"" + "x".repeat(501) + "\"}"), 400,
                "VALIDATION_ERROR")
                .andExpect(jsonPath("$.detail").value("reason: Reason must have at most 500 characters"));

        assertThat(CATEDRA.requestCount(cancelPath)).isZero();
    }

    // The cátedra has it in another status than the local process: nothing changes locally.
    @ParameterizedTest
    @CsvSource({
            "409, RESERVATION_NOT_CANCELLABLE, 409, RESERVATION_NOT_CANCELLABLE",
            "403, STUDENT_INTEGRATION_NOT_FOUND, 503, SERVICE_UNAVAILABLE",
            "404, RESERVATION_NOT_FOUND, 500, INTERNAL_ERROR",
            "403, RESERVATION_OWNER_MISMATCH, 500, INTERNAL_ERROR"
    })
    void translatesEachRejectionOfTheCatedra(int catedraStatus, String catedraCode, int status, String code)
            throws Exception {
        Long id = givenAProcess(ReservationStatus.CONFIRMED);
        CATEDRA.respondProblem(cancelPath, catedraStatus, catedraCode);

        expectProblem(cancel(processId, userExternalId, REASON), status, code);

        assertStoredStatus(id, ReservationStatus.CONFIRMED);
    }

    // Section 18.4: the reservations of the day show the first cancellation arrived, so it is not sent again.
    @Test
    void doesNotCancelAgainWhenTheCancellationWithoutAnswerArrived() throws Exception {
        Long id = givenAProcess(ReservationStatus.CONFIRMED);
        CATEDRA.respondAfter(cancelPath, TIMEOUT, 200, cancelled());
        CATEDRA.respond(APPOINTMENTS_PATH, 200, appointmentIn("CANCELLED"));

        expectCancelled(cancel(processId, userExternalId, REASON), id);

        assertThat(CATEDRA.requestCount(cancelPath)).isEqualTo(1);
        assertStoredStatus(id, ReservationStatus.CANCELLED);
    }

    @Test
    void answers503AndKeepsTheReservationConfirmedWhenTheCatedraDoesNotAnswer() throws Exception {
        Long id = givenAProcess(ReservationStatus.CONFIRMED);
        CATEDRA.respondAfter(cancelPath, TIMEOUT, 200, cancelled());
        CATEDRA.respond(APPOINTMENTS_PATH, 200, appointmentIn("CONFIRMED"));

        expectProblem(cancel(processId, userExternalId, REASON), 503, "CANCELLATION_PENDING");

        assertThat(CATEDRA.requestCount(cancelPath)).isEqualTo(2);
        assertStoredStatus(id, ReservationStatus.CONFIRMED);
    }

    @Test
    void answers401WithoutAUserToken() throws Exception {
        mockMvc.perform(post("/reservations/" + processId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON).content(REASON))
                .andExpect(status().isUnauthorized());

        assertThat(CATEDRA.requestCount(cancelPath)).isZero();
    }

    private Long givenAProcess(ReservationStatus status) {
        Instant created = Instant.parse("2000-01-01T09:50:00Z");
        return jpaReservationProcessRepository.save(ReservationProcessEntity.builder()
                .userExternalId(userExternalId).professionalId(15L)
                .date(LocalDate.of(2099, 9, 14)).startTime(LocalTime.of(9, 30))
                .status(status).holdId("hold-" + processId).reservationProcessId(processId)
                .requestEventId("request-event-8a31").expiresAt(Instant.parse("2099-09-10T14:44:50Z"))
                .active(!status.isFinal()).createdAt(created).updatedAt(created)
                .build()).getId();
    }

    private String cancelled() {
        return """
                {"reservationId":8251,"reservationProcessId":"%s","status":"CANCELLED",
                 "cancelledAt":"2099-09-11T15:10:00Z"}
                """.formatted(processId);
    }

    private String appointmentIn(String status) {
        return """
                [{"reservationId":8251,"reservationProcessId":"%s","externalPatientId":"%s","professionalId":15,
                  "appointmentDate":"2099-09-14","startTime":"09:30:00","status":"%s"}]
                """.formatted(processId, userExternalId, status);
    }

    private static void expectCancelled(ResultActions result, Long id) throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Cambio solicitado por el usuario"));
    }

    private void assertStoredStatus(Long id, ReservationStatus status) {
        assertThat(jpaReservationProcessRepository.findById(id))
                .hasValueSatisfying(process -> assertThat(process.getStatus()).isEqualTo(status));
    }

    private ResultActions cancel(String reservationProcessId, String subject, String body) throws Exception {
        return mockMvc.perform(withUser(post("/reservations/" + reservationProcessId + "/cancel"), subject)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static MockHttpServletRequestBuilder withUser(MockHttpServletRequestBuilder request, String subject) {
        return request.with(jwt().jwt(token -> token.subject(subject)));
    }
}
