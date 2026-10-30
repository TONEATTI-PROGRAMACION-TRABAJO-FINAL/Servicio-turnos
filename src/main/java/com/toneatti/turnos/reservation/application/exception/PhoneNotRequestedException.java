package com.toneatti.turnos.reservation.application.exception;

/**
 * The process is waiting for the phone, but the event of the cátedra that requested it has not arrived yet, and its
 * eventId is required to send the phone (section 15.5 of the integration contract).
 */
public class PhoneNotRequestedException extends RuntimeException {
    public PhoneNotRequestedException(Long id) {
        super("The cátedra has not requested the phone of reservation process " + id + " yet");
    }
}
