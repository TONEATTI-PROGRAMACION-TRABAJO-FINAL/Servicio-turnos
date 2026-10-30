package com.toneatti.turnos.reservation.application.exception;

public class HoldExpiredException extends RuntimeException {
    public HoldExpiredException(Long id) {
        super("The hold of reservation process " + id + " expired");
    }
}
