package com.toneatti.turnos.catedra.infrastructure.rest.config;

import com.toneatti.turnos.config.CatedraIntegrationProperties;
import com.toneatti.turnos.support.FakeHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatedraRestClientConfigTest {

    private static final Duration READ_TIMEOUT = Duration.ofMillis(500);
    private static final String PATH = "/api/appointment-occupancies";

    private FakeHttpServer catedra;
    private RestClient restClient;

    @BeforeEach
    void startCatedra() throws IOException {
        catedra = FakeHttpServer.start();
        CatedraIntegrationProperties properties = new CatedraIntegrationProperties(
                catedra.baseUrl(), "test-jwt", Duration.ofSeconds(1), READ_TIMEOUT,
                new CatedraIntegrationProperties.Kafka("localhost:9092", "group", "actions", "phone"));
        restClient = new CatedraRestClientConfig().catedraRestClient(properties);
    }

    @AfterEach
    void stopCatedra() {
        catedra.close();
    }

    @Test
    void sendsTheTechnicalJwtAsABearerTokenToTheApiBaseUrl() {
        catedra.respond(PATH, 200, "{}");

        restClient.get().uri(PATH).retrieve().toBodilessEntity();

        assertThat(catedra.lastRequestUri().getPath()).isEqualTo(PATH);
        assertThat(catedra.lastRequestHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-jwt");
    }

    @Test
    void failsWhenTheCatedraTakesLongerThanTheReadTimeout() {
        catedra.respondAfter(PATH, READ_TIMEOUT.multipliedBy(4), 200, "{}");

        assertThatThrownBy(() -> restClient.get().uri(PATH).retrieve().toBodilessEntity())
                .isInstanceOf(ResourceAccessException.class);
    }
}
