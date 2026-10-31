package com.toneatti.turnos.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Errors the controllers answer to the app: rendered as application/problem+json with a stable "code",
 * the same convention as the cátedra (section 13).
 */
public final class ProblemResponses {

    private ProblemResponses() {
    }

    public static ResponseStatusException problem(HttpStatus status, String code, String detail) {
        ResponseStatusException exception = new ResponseStatusException(status, detail);
        exception.getBody().setProperty("code", code);
        return exception;
    }
}
