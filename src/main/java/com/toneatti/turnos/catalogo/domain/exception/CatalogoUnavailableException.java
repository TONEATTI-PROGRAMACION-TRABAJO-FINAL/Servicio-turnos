package com.toneatti.turnos.catalogo.domain.exception;

/**
 * Servicio-catalogo could not answer: error status, timeout, refused connection or unreadable response.
 */
public class CatalogoUnavailableException extends RuntimeException {
    public CatalogoUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
