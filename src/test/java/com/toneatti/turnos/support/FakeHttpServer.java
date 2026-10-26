package com.toneatti.turnos.support;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Stand-in for an HTTP service consumed by this one (Servicio-catalogo or the cátedra) on a free local port,
 * built on the JDK HTTP server.
 * Each path answers with the canned response registered for it; unknown paths answer 404.
 */
public final class FakeHttpServer implements AutoCloseable {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String JSON = "application/json";

    private record CannedResponse(int status, String body, Duration delay) {
    }

    private final HttpServer server;
    private final Map<String, CannedResponse> responses = new ConcurrentHashMap<>();
    private volatile URI lastRequestUri;
    private volatile Headers lastRequestHeaders;

    private FakeHttpServer(HttpServer server) {
        this.server = server;
    }

    /**
     * Unchecked so it can also start a server in a static field initializer.
     */
    public static FakeHttpServer start() {
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        FakeHttpServer fake = new FakeHttpServer(server);
        server.createContext("/", fake::handle);
        // Virtual threads so a delayed response does not block the other requests.
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        return fake;
    }

    /**
     * A base URL where nothing is listening, to get a refused connection.
     */
    public static String unreachableBaseUrl() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return "http://127.0.0.1:" + socket.getLocalPort();
        }
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void respond(String path, int status, String body) {
        respondAfter(path, Duration.ZERO, status, body);
    }

    public void respondAfter(String path, Duration delay, int status, String body) {
        responses.put(path, new CannedResponse(status, body, delay));
    }

    public URI lastRequestUri() {
        return lastRequestUri;
    }

    /**
     * First value of that header in the last request, or null when it was not sent.
     */
    public String lastRequestHeader(String name) {
        return lastRequestHeaders.getFirst(name);
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        lastRequestUri = exchange.getRequestURI();
        lastRequestHeaders = exchange.getRequestHeaders();
        CannedResponse response = responses.getOrDefault(exchange.getRequestURI().getPath(),
                new CannedResponse(404, "{\"status\":404,\"title\":\"Not Found\"}", Duration.ZERO));
        try {
            Thread.sleep(response.delay());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", response.status() < 400 ? JSON : PROBLEM_JSON);
        // For the JDK server, length 0 means chunked; -1 means no body at all.
        exchange.sendResponseHeaders(response.status(), body.length == 0 ? -1 : body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
