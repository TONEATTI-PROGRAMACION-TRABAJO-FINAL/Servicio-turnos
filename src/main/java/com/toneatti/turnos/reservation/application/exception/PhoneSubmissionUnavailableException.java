package com.toneatti.turnos.reservation.application.exception;

/**
 * The phone number could not be delivered to the cátedra in time. A timeout does not prove it was not delivered
 * (section 18.4 of the integration contract); the process keeps waiting for the phone, so it can be sent again.
 */
public class PhoneSubmissionUnavailableException extends RuntimeException {
    public PhoneSubmissionUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
