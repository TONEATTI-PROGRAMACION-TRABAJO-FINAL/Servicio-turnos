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
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stand-in for an HTTP service consumed by this one (Servicio-catalogo or the cátedra) on a free local port,
 * built on the JDK HTTP server.
 * Each path answers first with its one-shot responses, in the order they were registered, and then with the canned
 * response registered for it; unknown paths answer 404.
 */
public final class FakeHttpServer implements AutoCloseable {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String JSON = "application/json";

    private record CannedResponse(int status, String body, Duration delay, Map<String, String> headers) {
    }

    private final HttpServer server;
    private final Map<String, CannedResponse> responses = new ConcurrentHashMap<>();
    private final Map<String, Queue<CannedResponse>> oneShotResponses = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private volatile URI lastRequestUri;
    private volatile Headers lastRequestHeaders;
    private volatile String lastRequestBody;

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
        responses.put(path, new CannedResponse(status, body, delay, Map.of()));
    }

    /**
     * Like {@link #respond(String, int, String)}, with extra response headers (for example X-Total-Count).
     */
    public void respond(String path, int status, Map<String, String> headers, String body) {
        responses.put(path, new CannedResponse(status, body, Duration.ZERO, headers));
    }

    /**
     * A rejection with a functional code, as the cátedra answers it (section 13 of the integration contract).
     */
    public void respondProblem(String path, int status, String code) {
        respond(path, status, "{\"status\":" + status + ",\"code\":\"" + code + "\"}");
    }

    /**
     * Answers the next request to that path only, before the canned response. Several calls queue up, so a test can
     * script a timeout followed by a success.
     */
    public void respondOnce(String path, int status, String body) {
        respondOnceAfter(path, Duration.ZERO, status, body);
    }

    public void respondOnceAfter(String path, Duration delay, int status, String body) {
        oneShotResponses.computeIfAbsent(path, ignored -> new ConcurrentLinkedQueue<>())
                .add(new CannedResponse(status, body, delay, Map.of()));
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

    /**
     * Body of the last request, empty when it had none.
     */
    public String lastRequestBody() {
        return lastRequestBody;
    }

    /**
     * Requests received for that path, including the ones the client gave up on, so a test can check that an
     * operation with effects was not repeated.
     */
    public int requestCount(String path) {
        AtomicInteger count = requestCounts.get(path);
        return count == null ? 0 : count.get();
    }

    /**
     * Forgets the responses and the request counts, for a server shared by the tests of a class.
     */
    public void reset() {
        responses.clear();
        oneShotResponses.clear();
        requestCounts.clear();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        lastRequestUri = exchange.getRequestURI();
        lastRequestHeaders = exchange.getRequestHeaders();
        lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String path = exchange.getRequestURI().getPath();
        requestCounts.computeIfAbsent(path, ignored -> new AtomicInteger()).incrementAndGet();
        Queue<CannedResponse> oneShot = oneShotResponses.get(path);
        CannedResponse queued = oneShot == null ? null : oneShot.poll();
        CannedResponse response = queued != null ? queued : responses.getOrDefault(path,
                new CannedResponse(404, "{\"status\":404,\"title\":\"Not Found\"}", Duration.ZERO, Map.of()));
        try {
            Thread.sleep(response.delay());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", response.status() < 400 ? JSON : PROBLEM_JSON);
        response.headers().forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
        // For the JDK server, length 0 means chunked; -1 means no body at all.
        exchange.sendResponseHeaders(response.status(), body.length == 0 ? -1 : body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
