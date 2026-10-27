package com.toneatti.turnos.catedra.infrastructure.rest.adapter;

import com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException;
import com.toneatti.turnos.catedra.domain.exception.CatedraUnavailableException;
import com.toneatti.turnos.catedra.domain.model.Hold;
import com.toneatti.turnos.catedra.domain.model.Occupancy;
import com.toneatti.turnos.catedra.domain.model.OccupancyStatus;
import com.toneatti.turnos.catedra.infrastructure.rest.config.CatedraRestClientConfig;
import com.toneatti.turnos.catedra.infrastructure.rest.mapper.CatedraRestMapper;
import com.toneatti.turnos.config.CatedraIntegrationProperties;
import com.toneatti.turnos.support.FakeHttpServer;
import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestCatedraRepositoryAdapterTest {

    private static final Duration READ_TIMEOUT = Duration.ofMillis(500);
    private static final String JWT = "test-technical-jwt";
    private static final String OCCUPANCIES_PATH = "/api/appointment-occupancies";
    private static final String HOLDS_PATH = "/api/appointment-holds";
    private static final LocalDate DAY = LocalDate.of(2026, 9, 14);
    private static final LocalTime NINE_THIRTY = LocalTime.of(9, 30);
    private static final String PATIENT = "user-42";

    private FakeHttpServer catedra;
    private RestCatedraRepositoryAdapter adapter;

    @BeforeEach
    void startCatedra() {
        catedra = FakeHttpServer.start();
        adapter = adapterFor(catedra.baseUrl());
    }

    @AfterEach
    void stopCatedra() {
        catedra.close();
    }

    @Test
    void findsTheOccupanciesOfAProfessional() {
        catedra.respond(OCCUPANCIES_PATH, 200, """
                {"professionalId":15,"from":"2026-09-14","to":"2026-09-15","occupancies":[
                  {"date":"2026-09-14","startTime":"09:00:00","status":"CONFIRMED","expiresAt":null},
                  {"date":"2026-09-15","startTime":"09:30:00","status":"HELD","expiresAt":"2026-09-10T14:44:50Z"}]}
                """);

        assertThat(adapter.findOccupancies(15L, DAY, DAY.plusDays(1))).containsExactly(
                new Occupancy(DAY, LocalTime.of(9, 0), OccupancyStatus.CONFIRMED, null),
                new Occupancy(DAY.plusDays(1), LocalTime.of(9, 30), OccupancyStatus.HELD,
                        Instant.parse("2026-09-10T14:44:50Z")));
        assertThat(catedra.lastRequestUri().getQuery()).isEqualTo("professionalId=15&from=2026-09-14&to=2026-09-15");
    }

    @Test
    void sendsTheTechnicalJwt() {
        catedra.respond(OCCUPANCIES_PATH, 200, "{\"occupancies\":[]}");

        adapter.findOccupancies(15L, DAY, DAY);

        assertThat(catedra.lastRequestHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + JWT);
    }

    @Test
    void acceptsStartTimesWithAndWithoutSeconds() {
        catedra.respond(OCCUPANCIES_PATH, 200, """
                {"occupancies":[
                  {"date":"2026-09-14","startTime":"09:30","status":"CONFIRMED","expiresAt":null},
                  {"date":"2026-09-14","startTime":"10:00:00","status":"CONFIRMED","expiresAt":null}]}
                """);

        assertThat(adapter.findOccupancies(15L, DAY, DAY)).extracting(Occupancy::startTime)
                .containsExactly(LocalTime.of(9, 30), LocalTime.of(10, 0));
    }

    @Test
    void ignoresUnknownFields() {
        catedra.respond(OCCUPANCIES_PATH, 200, """
                {"professionalId":15,"extra":"v1-compatible","occupancies":[
                  {"date":"2026-09-14","startTime":"09:00:00","status":"CONFIRMED","expiresAt":null,"source":"x"}]}
                """);

        assertThat(adapter.findOccupancies(15L, DAY, DAY)).hasSize(1);
    }

    @Test
    void findsNoOccupanciesWhenTheListIsEmpty() {
        catedra.respond(OCCUPANCIES_PATH, 200, """
                {"professionalId":15,"from":"2026-09-14","to":"2026-09-14","occupancies":[]}
                """);

        assertThat(adapter.findOccupancies(15L, DAY, DAY)).isEmpty();
    }

    @Test
    void findsNoOccupanciesWhenTheBodyIsEmpty() {
        catedra.respond(OCCUPANCIES_PATH, 200, "");

        assertThat(adapter.findOccupancies(15L, DAY, DAY)).isEmpty();
    }

    @Test
    void findsNoOccupanciesWhenTheListIsMissing() {
        catedra.respond(OCCUPANCIES_PATH, 200, "{\"professionalId\":15}");

        assertThat(adapter.findOccupancies(15L, DAY, DAY)).isEmpty();
    }

    @Test
    void failsAsRejectedWithTheCodeOfTheCatedra() {
        catedra.respond(OCCUPANCIES_PATH, 400, """
                {"title":"Bad Request","status":400,"detail":"from is after to",
                 "path":"/api/appointment-occupancies","code":"INVALID_DATE_RANGE"}
                """);

        assertThatThrownBy(() -> adapter.findOccupancies(15L, DAY.plusDays(1), DAY))
                .isInstanceOfSatisfying(CatedraRequestRejectedException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(400);
                    assertThat(e.getCode()).isEqualTo(CatedraRequestRejectedException.INVALID_DATE_RANGE);
                })
                .hasMessageContaining("GET " + OCCUPANCIES_PATH);
    }

    @Test
    void failsAsRejectedWhenTheIntegrationIsNotFound() {
        catedra.respond(OCCUPANCIES_PATH, 403, "{\"status\":403,\"code\":\"STUDENT_INTEGRATION_NOT_FOUND\"}");

        assertThatFindingTheOccupanciesOfADayFails()
                .isInstanceOfSatisfying(CatedraRequestRejectedException.class, e ->
                        assertThat(e.getCode()).isEqualTo("STUDENT_INTEGRATION_NOT_FOUND"));
    }

    @Test
    void failsAsUnavailableWhenTheJwtIsRejectedWithoutACode() {
        catedra.respond(OCCUPANCIES_PATH, 401, "{\"status\":401,\"title\":\"Unauthorized\"}");

        assertThatFindingTheOccupanciesOfADayFails()
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCauseInstanceOf(HttpClientErrorException.class)
                .message().doesNotContain(JWT);
    }

    @Test
    void failsAsUnavailableWhenAccessIsForbiddenWithoutACode() {
        catedra.respond(OCCUPANCIES_PATH, 403, "{\"status\":403,\"title\":\"Forbidden\"}");

        assertThatFindingTheOccupanciesOfADayFails()
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCauseInstanceOf(HttpClientErrorException.class);
    }

    @Test
    void failsAsUnavailableOnAClientErrorWithABlankCode() {
        catedra.respond(OCCUPANCIES_PATH, 400, "{\"status\":400,\"code\":\"\"}");

        assertThatFindingTheOccupanciesOfADayFails()
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCauseInstanceOf(HttpClientErrorException.class);
    }

    @Test
    void failsAsUnavailableOnAClientErrorWithAnUnreadableProblem() {
        catedra.respond(OCCUPANCIES_PATH, 400, "not json");

        assertThatFindingTheOccupanciesOfADayFails()
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCauseInstanceOf(HttpClientErrorException.class);
    }

    @Test
    void failsAsUnavailableOnAClientErrorWithoutABody() {
        catedra.respond(OCCUPANCIES_PATH, 400, "");

        assertThatFindingTheOccupanciesOfADayFails()
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCauseInstanceOf(HttpClientErrorException.class);
    }

    @Test
    void failsAsUnavailableOnAServerError() {
        catedra.respond(OCCUPANCIES_PATH, 503, "{\"status\":503,\"title\":\"Service Unavailable\"}");

        assertThatFindingTheOccupanciesOfADayFails()
                .isInstanceOf(CatedraUnavailableException.class)
                .hasMessageContaining("GET " + OCCUPANCIES_PATH)
                .hasCauseInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void failsAsUnavailableWhenTheCatedraTakesLongerThanTheReadTimeout() {
        catedra.respondAfter(OCCUPANCIES_PATH, READ_TIMEOUT.multipliedBy(4), 200, "{\"occupancies\":[]}");

        assertThatFindingTheOccupanciesOfADayFails()
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
    }

    @Test
    void failsAsUnavailableWhenTheConnectionIsRefused() throws IOException {
        RestCatedraRepositoryAdapter unreachable = adapterFor(FakeHttpServer.unreachableBaseUrl());

        assertThatThrownBy(() -> unreachable.findOccupancies(15L, DAY, DAY))
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
    }

    @Test
    void failsAsUnavailableWhenTheBodyIsNotJson() {
        catedra.respond(OCCUPANCIES_PATH, 200, "not json");

        assertThatFindingTheOccupanciesOfADayFails()
                .isInstanceOf(CatedraUnavailableException.class);
    }

    @Test
    void failsAsUnavailableWhenAStatusIsUnknown() {
        catedra.respond(OCCUPANCIES_PATH, 200, """
                {"occupancies":[{"date":"2026-09-14","startTime":"09:00:00","status":"CANCELLED","expiresAt":null}]}
                """);

        assertThatFindingTheOccupanciesOfADayFails()
                .isInstanceOf(CatedraUnavailableException.class);
    }

    @Test
    void createsAHold() {
        catedra.respond(HOLDS_PATH, 201, """
                {"holdId":"hold-6c0c2f41","reservationProcessId":"process-a92b47d0","status":"HELD",
                 "expiresAt":"2026-09-10T14:44:50Z"}
                """);

        assertThat(adapter.createHold(15L, DAY, NINE_THIRTY, PATIENT)).isEqualTo(
                new Hold("hold-6c0c2f41", "process-a92b47d0", Instant.parse("2026-09-10T14:44:50Z")));
        assertThat(catedra.lastRequestBody()).isEqualTo("""
                {"professionalId":15,"date":"2026-09-14","startTime":"09:30","externalPatientId":"user-42"}""");
        assertThat(catedra.lastRequestHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo("application/json");
    }

    // Every code of section 13.1 that creating a hold can answer.
    @ParameterizedTest
    @CsvSource({
            "400, VALIDATION_ERROR",
            "403, STUDENT_INTEGRATION_NOT_FOUND",
            "404, PROFESSIONAL_NOT_FOUND",
            "400, PROFESSIONAL_DISABLED",
            "400, INVALID_SLOT",
            "409, SLOT_ALREADY_RESERVED",
            "409, SLOT_ALREADY_HELD"
    })
    void failsAsRejectedWithEachCodeOfCreatingAHold(int status, String code) {
        catedra.respond(HOLDS_PATH, status, "{\"status\":" + status + ",\"code\":\"" + code + "\"}");

        assertThatCreatingAHoldFails()
                .isInstanceOfSatisfying(CatedraRequestRejectedException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(status);
                    assertThat(e.getCode()).isEqualTo(code);
                })
                .hasMessageContaining("POST " + HOLDS_PATH);
    }

    @Test
    void failsAsUnavailableWhenCreatingAHoldTakesLongerThanTheReadTimeout() {
        catedra.respondAfter(HOLDS_PATH, READ_TIMEOUT.multipliedBy(4), 201, "{\"holdId\":\"late\"}");

        assertThatCreatingAHoldFails()
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
        // The request reached the cátedra: the hold may exist even though the call failed.
        assertThat(catedra.requestCount(HOLDS_PATH)).isEqualTo(1);
    }

    @Test
    void failsAsUnavailableWhenAHoldIsCreatedWithoutABody() {
        catedra.respond(HOLDS_PATH, 201, "");

        assertThatCreatingAHoldFails()
                .isInstanceOf(CatedraUnavailableException.class)
                .hasMessageContaining("without a hold");
    }

    @Test
    void failsAsUnavailableWhenAHoldIsCreatedWithoutItsIds() {
        catedra.respond(HOLDS_PATH, 201, "{\"status\":\"HELD\",\"expiresAt\":\"2026-09-10T14:44:50Z\"}");

        assertThatCreatingAHoldFails()
                .isInstanceOf(CatedraUnavailableException.class)
                .hasMessageContaining("without a hold");
    }

    private AbstractThrowableAssert<?, ? extends Throwable> assertThatCreatingAHoldFails() {
        return assertThatThrownBy(() -> adapter.createHold(15L, DAY, NINE_THIRTY, PATIENT));
    }

    private AbstractThrowableAssert<?, ? extends Throwable> assertThatFindingTheOccupanciesOfADayFails() {
        return assertThatThrownBy(() -> adapter.findOccupancies(15L, DAY, DAY));
    }

    private static RestCatedraRepositoryAdapter adapterFor(String baseUrl) {
        CatedraIntegrationProperties properties = new CatedraIntegrationProperties(
                baseUrl, JWT, Duration.ofSeconds(1), READ_TIMEOUT,
                new CatedraIntegrationProperties.Kafka("localhost:9092", "group", "actions", "phone"));
        return new RestCatedraRepositoryAdapter(
                new CatedraRestClientConfig().catedraRestClient(properties), new CatedraRestMapper());
    }
}
