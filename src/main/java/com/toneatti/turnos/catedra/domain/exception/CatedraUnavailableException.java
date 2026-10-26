package com.toneatti.turnos.catedra.domain.exception;

/**
 * The cátedra could not answer: server error, timeout, refused connection, unreadable response,
 * or a rejection without a functional {@code code} (for example, the technical JWT was not accepted).
 */
public class CatedraUnavailableException extends RuntimeException {
    public CatedraUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
