package com.toneatti.turnos.availability.application.exception;

/**
 * The availability cannot be calculated now because Servicio-catalogo or the cátedra could not answer.
 */
public class AvailabilityUnavailableException extends RuntimeException {
    public AvailabilityUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
