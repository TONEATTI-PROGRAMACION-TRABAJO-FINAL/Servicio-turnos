package com.toneatti.turnos.reservation.application.exception;

import com.toneatti.turnos.reservation.domain.model.ReservationStatus;

public class ReservationNotWaitingForPhoneException extends RuntimeException {
    public ReservationNotWaitingForPhoneException(Long id, ReservationStatus status) {
        super("Reservation process " + id + " is not waiting for a phone but " + status);
    }
}
