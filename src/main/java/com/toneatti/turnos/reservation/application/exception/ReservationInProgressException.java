package com.toneatti.turnos.reservation.application.exception;

/**
 * The user already has an active reservation process for the slot; two concurrent requests both passed the check.
 */
public class ReservationInProgressException extends RuntimeException {
    public ReservationInProgressException(String userExternalId, Throwable cause) {
        super("User " + userExternalId + " already has an active reservation process for the slot", cause);
    }
}
