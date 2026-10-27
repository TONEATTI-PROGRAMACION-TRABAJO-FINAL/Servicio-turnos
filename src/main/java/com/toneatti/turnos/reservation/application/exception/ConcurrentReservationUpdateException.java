package com.toneatti.turnos.reservation.application.exception;

/**
 * Another request changed the reservation process after it was read, so this change was not saved.
 */
public class ConcurrentReservationUpdateException extends RuntimeException {
    public ConcurrentReservationUpdateException(Long id, Throwable cause) {
        super("Reservation process " + id + " was changed by another request", cause);
    }
}
