package com.toneatti.turnos.reservation.application.exception;

/**
 * The cátedra did not answer the cancellation and it is not known whether it happened. The process stays CONFIRMED:
 * cancelling again is safe, and if the cancellation did happen the AppointmentCancelled event closes the process.
 */
public class CancellationPendingException extends RuntimeException {
    public CancellationPendingException(Long id, Throwable cause) {
        super("The cancellation of reservation process " + id + " is not known yet", cause);
    }
}
