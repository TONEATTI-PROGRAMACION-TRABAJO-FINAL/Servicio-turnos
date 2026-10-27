package com.toneatti.turnos.reservation.application.exception;

/**
 * The cátedra did not answer and it is not known whether the last operation had effects. The process stays active,
 * so starting the same reservation again resumes it.
 */
public class ReservationPendingException extends RuntimeException {

    private final Long id;

    public ReservationPendingException(Long id, Throwable cause) {
        super("The outcome of reservation process " + id + " is not known yet", cause);
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
