package com.toneatti.turnos.reservation.application.exception;

public class ReservationExpiredException extends RuntimeException {
    public ReservationExpiredException(Long id) {
        super("Reservation process " + id + " expired");
    }
}
