package com.toneatti.turnos.reservation.infrastructure.web.controller;

import com.toneatti.turnos.reservation.domain.model.ReservationStatus;
import com.toneatti.turnos.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import com.toneatti.turnos.reservation.infrastructure.persistence.repository.JpaReservationProcessRepository;
import com.toneatti.turnos.support.FakeHttpServer;
import com.toneatti.turnos.user.infrastructure.persistence.entity.UserEntity;
import com.toneatti.turnos.user.infrastructure.persistence.repository.JpaUserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import static com.toneatti.turnos.support.ProblemAssertions.expectProblem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /reservations against a fake cátedra. The slots are in 2099 so none has started yet.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationIntegrationTest {

    // From application-test.properties.
    private static final String CATEDRA_JWT = "test-jwt";
    private static final Duration READ_TIMEOUT = Duration.ofMillis(500);
    private static final String HOLDS_PATH = "/api/appointment-holds";
    private static final String CONFIRM_PATH = HOLDS_PATH + "/hold-6c0c2f41/confirm";
    private static final String OCCUPANCIES_PATH = "/api/appointment-occupancies";
    private static final String APPOINTMENTS_PATH = "/api/appointments";
    // Longer than the read timeout: the request reaches the cátedra but its answer is lost.
    private static final Duration TIMEOUT = READ_TIMEOUT.multipliedBy(4);
    private static final String SLOT = """
            {"professionalId":15,"date":"2099-09-14","startTime":"09:30"}
            """;
    private static final String HOLD = """
            {"holdId":"hold-6c0c2f41","reservationProcessId":"process-a92b47d0","status":"HELD",
             "expiresAt":"2099-09-10T14:44:50Z"}
            """;
    private static final String HELD_SLOT = """
            {"occupancies":[{"date":"2099-09-14","startTime":"09:30:00","status":"HELD",
              "expiresAt":"2099-09-10T14:44:50Z"}]}
            """;
    private static final String CONFIRMATION = """
            {"reservationProcessId":"process-a92b47d0","status":"WAITING_FOR_PHONE","expiresAt":"2099-09-10T14:50:00Z"}
            """;

    private static final FakeHttpServer CATEDRA = FakeHttpServer.start();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JpaUserRepository jpaUserRepository;

    @Autowired
    private JpaReservationProcessRepository jpaReservationProcessRepository;

    private String userExternalId;

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
    void registerUser() {
        CATEDRA.reset();
        userExternalId = UUID.randomUUID().toString();
        jpaUserRepository.save(UserEntity.builder()
                .externalId(userExternalId)
                .email(userExternalId + "@example.com")
                .passwordHash("hash")
                .firstName("Juan")
                .lastName("Perez")
                .createdAt(Instant.now())
                .build());
    }

    @AfterEach
    void deleteData() {
        jpaReservationProcessRepository.deleteAll();
        jpaUserRepository.deleteAll();
    }

    @Test
    void holdsAndConfirmsTheSlotForTheAuthenticatedUser() throws Exception {
        givenTheCatedraHoldsAndConfirms();

        startReservation(SLOT)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/reservations/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.professionalId").value(15))
                .andExpect(jsonPath("$.date").value("2099-09-14"))
                .andExpect(jsonPath("$.startTime").value("09:30"))
                .andExpect(jsonPath("$.status").value("WAITING_FOR_PHONE"))
                .andExpect(jsonPath("$.expiresAt").value("2099-09-10T14:50:00Z"))
                .andExpect(jsonPath("$.holdId").doesNotExist())
                .andExpect(content().string(not(containsString(CATEDRA_JWT))));
        assertThat(CATEDRA.lastRequestHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + CATEDRA_JWT);
        ReservationProcessEntity stored = jpaReservationProcessRepository.findAll().getFirst();
        assertThat(stored.getUserExternalId()).isEqualTo(userExternalId);
        assertThat(stored.getHoldId()).isEqualTo("hold-6c0c2f41");
        assertThat(stored.getReservationProcessId()).isEqualTo("process-a92b47d0");
    }

    // The patient is the subject of the token and its registration, whatever the app sends.
    @Test
    void sendsThePatientOfTheTokenAndItsRegistrationToTheCatedra() throws Exception {
        givenTheCatedraHoldsAndConfirms();
        CATEDRA.respond(HOLDS_PATH, 201, HOLD);

        startReservation("""
                {"professionalId":15,"date":"2099-09-14","startTime":"09:30","externalPatientId":"someone-else",
                 "patientFirstName":"Otro","patientLastName":"Nombre"}
                """).andExpect(status().isCreated());

        assertThat(CATEDRA.lastRequestBody()).isEqualTo("""
                {"externalPatientId":"%s","patientFirstName":"Juan","patientLastName":"Perez"}"""
                .formatted(userExternalId));
    }

    @Test
    void resumesTheActiveProcessOfTheSlotInsteadOfHoldingItAgain() throws Exception {
        givenTheCatedraHoldsAndConfirms();
        String first = startReservation(SLOT).andReturn().getResponse().getContentAsString();

        startReservation(SLOT)
                .andExpect(status().isOk())
                .andExpect(content().json(first));
        assertThat(CATEDRA.requestCount(HOLDS_PATH)).isEqualTo(1);
        assertThat(CATEDRA.requestCount(CONFIRM_PATH)).isEqualTo(1);
    }

    // Every code of section 13.1 that creating a hold can answer, and what the app receives.
    @ParameterizedTest
    @CsvSource({
            "404, PROFESSIONAL_NOT_FOUND, 404, PROFESSIONAL_NOT_FOUND",
            "400, PROFESSIONAL_DISABLED, 400, PROFESSIONAL_DISABLED",
            "400, INVALID_SLOT, 400, INVALID_SLOT",
            "409, SLOT_ALREADY_RESERVED, 409, SLOT_ALREADY_RESERVED",
            "409, SLOT_ALREADY_HELD, 409, SLOT_ALREADY_HELD",
            "403, STUDENT_INTEGRATION_NOT_FOUND, 503, SERVICE_UNAVAILABLE",
            "400, VALIDATION_ERROR, 500, INTERNAL_ERROR"
    })
    void translatesEachRejectionOfTheHold(int catedraStatus, String catedraCode, int status, String code)
            throws Exception {
        CATEDRA.respondProblem(HOLDS_PATH, catedraStatus, catedraCode);

        expectProblem(startReservation(SLOT), status, code);
        assertTheProcessIs(ReservationStatus.FAILED);
        assertThat(CATEDRA.requestCount(CONFIRM_PATH)).isZero();
    }

    // Every code of section 13.1 that confirming a hold can answer, and what the app receives.
    @ParameterizedTest
    @CsvSource({
            "404, PROFESSIONAL_NOT_FOUND, 404, PROFESSIONAL_NOT_FOUND, FAILED",
            "400, INVALID_SLOT, 400, INVALID_SLOT, FAILED",
            "409, SLOT_ALREADY_RESERVED, 409, SLOT_ALREADY_RESERVED, FAILED",
            "404, HOLD_NOT_FOUND, 410, HOLD_NOT_FOUND, FAILED",
            "404, HOLD_EXPIRED, 410, HOLD_EXPIRED, EXPIRED",
            "403, HOLD_OWNER_MISMATCH, 500, INTERNAL_ERROR, FAILED",
            "400, EXTERNAL_PATIENT_MISMATCH, 500, INTERNAL_ERROR, FAILED",
            "403, STUDENT_INTEGRATION_NOT_FOUND, 503, SERVICE_UNAVAILABLE, FAILED",
            "400, VALIDATION_ERROR, 500, INTERNAL_ERROR, FAILED"
    })
    void translatesEachRejectionOfTheConfirmation(int catedraStatus, String catedraCode, int status, String code,
                                                  ReservationStatus processStatus) throws Exception {
        CATEDRA.respond(HOLDS_PATH, 201, HOLD);
        CATEDRA.respondProblem(CONFIRM_PATH, catedraStatus, catedraCode);

        expectProblem(startReservation(SLOT), status, code);
        assertTheProcessIs(processStatus);
    }

    @Test
    void confirmsWhenTheCatedraSaysTheProcessWasAlreadyConfirmed() throws Exception {
        CATEDRA.respond(HOLDS_PATH, 201, HOLD);
        CATEDRA.respondProblem(CONFIRM_PATH, 409, "RESERVATION_PROCESS_ALREADY_CONFIRMED");

        startReservation(SLOT)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING_FOR_PHONE"));
    }

    @Test
    void answers503PendingWithoutDetailsOfTheCatedraWhenItDoesNotAnswer() throws Exception {
        CATEDRA.respond(HOLDS_PATH, 503, "{\"status\":503,\"detail\":\"redis down\"}");

        expectProblem(startReservation(SLOT), 503, "RESERVATION_PENDING")
                .andExpect(jsonPath("$.reservationId").isNumber())
                .andExpect(content().string(not(containsString("redis down"))))
                .andExpect(content().string(not(containsString(CATEDRA_JWT))));
        assertTheProcessIs(ReservationStatus.PENDING_HOLD);
    }

    // Section 18.4: the hold timed out, the slot is still free, so it is requested again.
    @Test
    void requestsTheHoldAgainAfterATimeoutWhenTheSlotIsFree() throws Exception {
        CATEDRA.respondOnceAfter(HOLDS_PATH, TIMEOUT, 201, HOLD);
        givenTheCatedraHoldsAndConfirms();
        CATEDRA.respond(OCCUPANCIES_PATH, 200, "{\"occupancies\":[]}");

        startReservation(SLOT)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING_FOR_PHONE"));
        assertThat(CATEDRA.requestCount(OCCUPANCIES_PATH)).isEqualTo(1);
        assertThat(CATEDRA.requestCount(HOLDS_PATH)).isEqualTo(2);
    }

    // The timed out hold may be the one that now occupies the slot: it is not requested again.
    @Test
    void answers503PendingAfterATimeoutWhenTheSlotIsHeld() throws Exception {
        CATEDRA.respondAfter(HOLDS_PATH, TIMEOUT, 201, HOLD);
        CATEDRA.respond(OCCUPANCIES_PATH, 200, HELD_SLOT);

        expectProblem(startReservation(SLOT), 503, "RESERVATION_PENDING")
                .andExpect(jsonPath("$.reservationId").isNumber());
        expectProblem(startReservation(SLOT), 503, "RESERVATION_PENDING");

        assertThat(CATEDRA.requestCount(HOLDS_PATH)).isEqualTo(1);
        assertThat(CATEDRA.requestCount(OCCUPANCIES_PATH)).isEqualTo(2);
        assertTheProcessIs(ReservationStatus.PENDING_HOLD);
    }

    @Test
    void resumesAPendingHoldOnceTheSlotIsFree() throws Exception {
        CATEDRA.respondOnceAfter(HOLDS_PATH, TIMEOUT, 201, HOLD);
        CATEDRA.respondOnce(OCCUPANCIES_PATH, 200, HELD_SLOT);
        expectProblem(startReservation(SLOT), 503, "RESERVATION_PENDING");
        givenTheCatedraHoldsAndConfirms();
        CATEDRA.respond(OCCUPANCIES_PATH, 200, "{\"occupancies\":[]}");

        startReservation(SLOT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_FOR_PHONE"));
        assertThat(CATEDRA.requestCount(HOLDS_PATH)).isEqualTo(2);
    }

    // The confirmation timed out but reached the cátedra: it is not sent again.
    @Test
    void doesNotConfirmAgainAfterATimeoutWhenTheReservationExists() throws Exception {
        CATEDRA.respond(HOLDS_PATH, 201, HOLD);
        CATEDRA.respondAfter(CONFIRM_PATH, TIMEOUT, 202, CONFIRMATION);
        CATEDRA.respond(APPOINTMENTS_PATH, 200, """
                [{"reservationId":8251,"reservationProcessId":"process-a92b47d0","externalPatientId":"%s",
                  "appointmentDate":"2099-09-14","startTime":"09:30:00","professionalId":15,"status":"PHONE_PENDING"}]
                """.formatted(userExternalId));

        startReservation(SLOT)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING_FOR_PHONE"))
                .andExpect(jsonPath("$.expiresAt").value("2099-09-10T14:44:50Z"));
        assertThat(CATEDRA.requestCount(CONFIRM_PATH)).isEqualTo(1);
        assertThat(CATEDRA.lastRequestUri().getQuery()).startsWith("externalPatientId=" + userExternalId);
    }

    @Test
    void confirmsAgainAfterATimeoutWhenTheReservationDoesNotExist() throws Exception {
        CATEDRA.respondOnceAfter(CONFIRM_PATH, TIMEOUT, 202, CONFIRMATION);
        givenTheCatedraHoldsAndConfirms();
        CATEDRA.respond(APPOINTMENTS_PATH, 200, "[]");

        startReservation(SLOT)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING_FOR_PHONE"));
        assertThat(CATEDRA.requestCount(APPOINTMENTS_PATH)).isEqualTo(1);
        assertThat(CATEDRA.requestCount(CONFIRM_PATH)).isEqualTo(2);
    }

    // The first confirmation arrived after the reservations were read: the repeated one finds it confirmed.
    @Test
    void confirmsWhenTheRepeatedConfirmationFindsItAlreadyConfirmed() throws Exception {
        CATEDRA.respond(HOLDS_PATH, 201, HOLD);
        CATEDRA.respondOnceAfter(CONFIRM_PATH, TIMEOUT, 202, CONFIRMATION);
        CATEDRA.respondProblem(CONFIRM_PATH, 409, "RESERVATION_PROCESS_ALREADY_CONFIRMED");
        CATEDRA.respond(APPOINTMENTS_PATH, 200, "[]");

        startReservation(SLOT)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING_FOR_PHONE"));
        assertThat(CATEDRA.requestCount(CONFIRM_PATH)).isEqualTo(2);
    }

    @Test
    void answers503PendingWhenEveryConfirmationTimesOut() throws Exception {
        CATEDRA.respond(HOLDS_PATH, 201, HOLD);
        CATEDRA.respondAfter(CONFIRM_PATH, TIMEOUT, 202, CONFIRMATION);
        CATEDRA.respond(APPOINTMENTS_PATH, 200, "[]");

        expectProblem(startReservation(SLOT), 503, "RESERVATION_PENDING");

        assertThat(CATEDRA.requestCount(CONFIRM_PATH)).isEqualTo(2);
        assertTheProcessIs(ReservationStatus.CONFIRMING);
    }

    @Test
    void answers400WhenAFieldIsMissing() throws Exception {
        expectProblem(startReservation("{\"date\":\"2099-09-14\",\"startTime\":\"09:30\"}"), 400, "VALIDATION_ERROR")
                .andExpect(jsonPath("$.detail").value(containsString("professionalId")));
        assertThat(CATEDRA.requestCount(HOLDS_PATH)).isZero();
    }

    @Test
    void answers400WhenATimeIsNotInTheContractFormat() throws Exception {
        expectProblem(startReservation("{\"professionalId\":15,\"date\":\"2099-09-14\",\"startTime\":\"9.30\"}"),
                400, "VALIDATION_ERROR");
    }

    @Test
    void answers400WhenTheSlotAlreadyStarted() throws Exception {
        expectProblem(startReservation("{\"professionalId\":15,\"date\":\"2000-01-03\",\"startTime\":\"09:30\"}"),
                400, "SLOT_IN_THE_PAST");
        assertThat(CATEDRA.requestCount(HOLDS_PATH)).isZero();
    }

    @Test
    void answers404WhenTheUserOfTheTokenDoesNotExist() throws Exception {
        expectProblem(mockMvc.perform(post("/reservations")
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SLOT)), 404, "USER_NOT_FOUND");
        assertThat(CATEDRA.requestCount(HOLDS_PATH)).isZero();
    }

    @Test
    void answers401WithoutAUserToken() throws Exception {
        mockMvc.perform(post("/reservations").contentType(MediaType.APPLICATION_JSON).content(SLOT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsAReservationOfTheUserAsItWasStarted() throws Exception {
        givenTheCatedraHoldsAndConfirms();
        String started = startReservation(SLOT).andReturn().getResponse().getContentAsString();
        Long id = jpaReservationProcessRepository.findAll().getFirst().getId();

        getReservation(id, userExternalId)
                .andExpect(status().isOk())
                .andExpect(content().json(started));
    }

    @Test
    void answers404ForAReservationOfAnotherUser() throws Exception {
        givenTheCatedraHoldsAndConfirms();
        startReservation(SLOT).andExpect(status().isCreated());
        Long id = jpaReservationProcessRepository.findAll().getFirst().getId();

        expectProblem(getReservation(id, UUID.randomUUID().toString()), 404, "RESERVATION_NOT_FOUND");
    }

    @Test
    void answers404ForAReservationThatDoesNotExist() throws Exception {
        expectProblem(getReservation(999_999L, userExternalId), 404, "RESERVATION_NOT_FOUND");
    }

    // The hold of a reservation whose confirmation got no answer is gone once its deadline passed.
    @Test
    void showsAsExpiredAReservationWhoseHoldExpired() throws Exception {
        Instant past = Instant.parse("2000-01-01T10:00:00Z");
        Long id = jpaReservationProcessRepository.save(ReservationProcessEntity.builder()
                .userExternalId(userExternalId).professionalId(15L)
                .date(LocalDate.of(2099, 9, 14)).startTime(LocalTime.of(9, 30))
                .status(ReservationStatus.CONFIRMING).holdId("hold-old").reservationProcessId("process-old")
                .expiresAt(past).active(true).createdAt(past).updatedAt(past)
                .build()).getId();

        getReservation(id, userExternalId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"));
        assertTheProcessIs(ReservationStatus.EXPIRED);
        assertThat(CATEDRA.requestCount(CONFIRM_PATH)).isZero();
    }

    // What the events of the cátedra left: a rejected phone, then the confirmation and its cancellation.
    @Test
    void showsTheOutcomeOfTheCatedraWithoutItsIds() throws Exception {
        Instant created = Instant.parse("2099-09-10T14:30:00Z");
        Long id = jpaReservationProcessRepository.save(ReservationProcessEntity.builder()
                .userExternalId(userExternalId).professionalId(15L)
                .date(LocalDate.of(2099, 9, 14)).startTime(LocalTime.of(9, 30))
                .status(ReservationStatus.CANCELLED).holdId("hold-6c0c2f41").reservationProcessId("process-a92b47d0")
                .requestEventId("request-event-8a31").catedraReservationId(8251L)
                .phoneRejectionReason("INVALID_PHONE_NUMBER").cancellationReason("Cambio solicitado por el usuario")
                .expiresAt(created.plusSeconds(890)).createdAt(created).updatedAt(created)
                .build()).getId();

        getReservation(id, userExternalId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.failureCode").isEmpty())
                .andExpect(jsonPath("$.phoneRejectionReason").value("INVALID_PHONE_NUMBER"))
                .andExpect(jsonPath("$.cancellationReason").value("Cambio solicitado por el usuario"))
                .andExpect(content().string(not(containsString("request-event-8a31"))))
                .andExpect(content().string(not(containsString("8251"))));
    }

    @Test
    void answers401ToGetAReservationWithoutAUserToken() throws Exception {
        mockMvc.perform(get("/reservations/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsTheReservationsOfTheAuthenticatedUserWithEveryFilter() throws Exception {
        CATEDRA.respond(APPOINTMENTS_PATH, 200, Map.of("X-Total-Count", "37"), """
                [{"reservationId":8251,"reservationProcessId":"process-a92b47d0","externalPatientId":"%s",
                  "professionalId":15,"professionalFirstName":"Ana","professionalLastName":"Gomez",
                  "appointmentDate":"2026-09-14","startTime":"09:30:00","endTime":"10:00:00",
                  "patientFirstName":"Juan","patientLastName":"Perez","patientPhone":"+5492615551234",
                  "status":"CONFIRMED","createdAt":"2026-09-10T14:35:00Z","confirmedAt":"2026-09-10T14:36:00Z",
                  "cancelledAt":null,"cancellationReason":null}]
                """.formatted(userExternalId));

        listReservations(userExternalId, "status=CONFIRMED&from=2026-09-01&to=2026-09-30&page=0&size=20")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(37))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content[0].reservationProcessId").value("process-a92b47d0"))
                .andExpect(jsonPath("$.content[0].professionalFirstName").value("Ana"))
                .andExpect(jsonPath("$.content[0].professionalLastName").value("Gomez"))
                .andExpect(jsonPath("$.content[0].startTime").value("09:30"))
                .andExpect(jsonPath("$.content[0].endTime").value("10:00"))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.content[0].reservationId").doesNotExist())
                .andExpect(content().string(not(containsString("+5492615551234"))));
        assertThat(CATEDRA.lastRequestUri().getQuery()).isEqualTo("externalPatientId=" + userExternalId
                + "&from=2026-09-01&to=2026-09-30&status=CONFIRMED&page=0&size=20");
    }

    @Test
    void usesDefaultPagination() throws Exception {
        CATEDRA.respond(APPOINTMENTS_PATH, 200, "[]");

        listReservations(userExternalId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
        assertThat(CATEDRA.lastRequestUri().getQuery())
                .isEqualTo("externalPatientId=" + userExternalId + "&page=0&size=20");
    }

    // The core guarantee: whatever the app sends, the patient forwarded to the cátedra is always the JWT's subject.
    @Test
    void neverForwardsAnExternalPatientIdOtherThanTheAuthenticatedUsers() throws Exception {
        CATEDRA.respond(APPOINTMENTS_PATH, 200, "[]");

        mockMvc.perform(get("/reservations").param("externalPatientId", "someone-else")
                        .with(jwt().jwt(token -> token.subject(userExternalId))))
                .andExpect(status().isOk());

        assertThat(CATEDRA.lastRequestUri().getQuery())
                .contains("externalPatientId=" + userExternalId)
                .doesNotContain("someone-else");
    }

    // Two different users never share the patient forwarded to the cátedra.
    @Test
    void forwardsADifferentExternalPatientIdPerUser() throws Exception {
        CATEDRA.respond(APPOINTMENTS_PATH, 200, "[]");
        String otherUser = UUID.randomUUID().toString();

        listReservations(userExternalId).andExpect(status().isOk());
        assertThat(CATEDRA.lastRequestUri().getQuery()).contains("externalPatientId=" + userExternalId);

        listReservations(otherUser).andExpect(status().isOk());
        assertThat(CATEDRA.lastRequestUri().getQuery())
                .contains("externalPatientId=" + otherUser)
                .doesNotContain(userExternalId);
    }

    @Test
    void answers400WhenFromIsAfterTo() throws Exception {
        expectProblem(listReservations(userExternalId, "from=2026-09-30&to=2026-09-01"), 400, "INVALID_DATE_RANGE");
        assertThat(CATEDRA.requestCount(APPOINTMENTS_PATH)).isZero();
    }

    @Test
    void answers400WhenPageIsNegative() throws Exception {
        expectProblem(listReservations(userExternalId, "page=-1"), 400, "VALIDATION_ERROR");
        assertThat(CATEDRA.requestCount(APPOINTMENTS_PATH)).isZero();
    }

    @ParameterizedTest
    @CsvSource({"0", "101"})
    void answers400WhenSizeIsOutOfRange(int size) throws Exception {
        expectProblem(listReservations(userExternalId, "size=" + size), 400, "VALIDATION_ERROR");
        assertThat(CATEDRA.requestCount(APPOINTMENTS_PATH)).isZero();
    }

    // STUDENT_INTEGRATION_NOT_FOUND is a configuration problem of the technical account, not the user's: it
    // surfaces as unavailable instead of a rejection (section 13.1), the same as T6.
    @Test
    void answers503WhenTheTechnicalIntegrationIsNotFound() throws Exception {
        CATEDRA.respondProblem(APPOINTMENTS_PATH, 403, "STUDENT_INTEGRATION_NOT_FOUND");

        expectProblem(listReservations(userExternalId), 503, "SERVICE_UNAVAILABLE")
                .andExpect(content().string(not(containsString("STUDENT_INTEGRATION_NOT_FOUND"))));
    }

    @Test
    void answers503WhenTheCatedraDoesNotAnswer() throws Exception {
        CATEDRA.respondAfter(APPOINTMENTS_PATH, TIMEOUT, 200, "[]");

        expectProblem(listReservations(userExternalId), 503, "SERVICE_UNAVAILABLE");
    }

    @Test
    void answers401ToListReservationsWithoutAUserToken() throws Exception {
        mockMvc.perform(get("/reservations"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions listReservations(String subject) throws Exception {
        return mockMvc.perform(get("/reservations").with(jwt().jwt(token -> token.subject(subject))));
    }

    private ResultActions listReservations(String subject, String query) throws Exception {
        return mockMvc.perform(get("/reservations?" + query).with(jwt().jwt(token -> token.subject(subject))));
    }

    private ResultActions getReservation(Long id, String subject) throws Exception {
        return mockMvc.perform(get("/reservations/" + id).with(jwt().jwt(token -> token.subject(subject))));
    }

    private void givenTheCatedraHoldsAndConfirms() {
        CATEDRA.respond(HOLDS_PATH, 201, HOLD);
        CATEDRA.respond(CONFIRM_PATH, 202, CONFIRMATION);
    }

    private ResultActions startReservation(String body) throws Exception {
        return mockMvc.perform(post("/reservations")
                .with(jwt().jwt(token -> token.subject(userExternalId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private void assertTheProcessIs(ReservationStatus status) {
        assertThat(jpaReservationProcessRepository.findAll())
                .singleElement()
                .satisfies(process -> {
                    assertThat(process.getStatus()).isEqualTo(status);
                    assertThat(process.getUserExternalId()).isEqualTo(userExternalId);
                });
    }
}
