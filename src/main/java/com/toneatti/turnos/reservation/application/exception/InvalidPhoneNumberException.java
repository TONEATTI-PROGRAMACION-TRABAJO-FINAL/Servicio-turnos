package com.toneatti.turnos.reservation.application.exception;

/**
 * The phone number is not valid for the cátedra (section 15.5 of the integration contract), so it is not sent.
 * The number is not part of the message: it is personal data.
 */
public class InvalidPhoneNumberException extends RuntimeException {
    public InvalidPhoneNumberException() {
        super("The phone number is not valid");
    }
}
