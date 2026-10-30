package com.toneatti.turnos.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

class FakeHttpServerTest {

    private static final String PATH = "/api/appointment-holds";

    private final FakeHttpServer server = FakeHttpServer.start();
    private final HttpClient client = HttpClient.newHttpClient();

    @AfterEach
    void stopServer() {
        server.close();
    }

    @Test
    void answersWithTheOneShotResponsesInOrderAndThenWithTheCannedOne() throws Exception {
        server.respond(PATH, 201, "{\"holdId\":\"canned\"}");
        server.respondOnce(PATH, 503, "{\"status\":503}");
        server.respondOnce(PATH, 409, "{\"code\":\"SLOT_ALREADY_HELD\"}");

        assertThat(post("{}").statusCode()).isEqualTo(503);
        assertThat(post("{}").statusCode()).isEqualTo(409);
        assertThat(post("{}").body()).isEqualTo("{\"holdId\":\"canned\"}");
        assertThat(post("{}").statusCode()).isEqualTo(201);
    }

    @Test
    void recordsTheBodyOfTheLastRequest() throws Exception {
        server.respond(PATH, 201, "{}");

        post("{\"externalPatientId\":\"user-42\"}");

        assertThat(server.lastRequestBody()).isEqualTo("{\"externalPatientId\":\"user-42\"}");
    }

    @Test
    void countsTheRequestsOfEachPath() throws Exception {
        server.respond(PATH, 201, "{}");

        post("{}");
        post("{}");

        assertThat(server.requestCount(PATH)).isEqualTo(2);
        assertThat(server.requestCount("/api/appointments")).isZero();
    }

    private HttpResponse<String> post(String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(server.baseUrl() + PATH))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
