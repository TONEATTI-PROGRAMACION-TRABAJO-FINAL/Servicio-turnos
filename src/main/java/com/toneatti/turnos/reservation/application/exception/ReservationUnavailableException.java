package com.toneatti.turnos.reservation.application.exception;

/**
 * The cátedra rejected the technical account of the project, so no reservation can be made until it is fixed.
 */
public class ReservationUnavailableException extends RuntimeException {
    public ReservationUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
