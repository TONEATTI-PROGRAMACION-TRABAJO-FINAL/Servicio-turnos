package com.toneatti.turnos.reservation.application.exception;

/**
 * The cátedra rejected a request this service should never send (for example VALIDATION_ERROR or
 * EXTERNAL_PATIENT_MISMATCH): a bug of this service, not something the user can fix.
 */
public class UnexpectedCatedraRejectionException extends RuntimeException {
    public UnexpectedCatedraRejectionException(String code, Throwable cause) {
        super("Catedra rejected the reservation with an unexpected " + code, cause);
    }
}
