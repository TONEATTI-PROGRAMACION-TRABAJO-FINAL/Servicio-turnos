package com.toneatti.turnos.reservation.application.exception;

import com.toneatti.turnos.reservation.domain.model.ReservationStatus;

/**
 * Only a CONFIRMED reservation can be cancelled (section 12 of the integration contract): the local process is in
 * another status, or the cátedra answered RESERVATION_NOT_CANCELLABLE.
 */
public class ReservationNotCancellableException extends RuntimeException {

    public ReservationNotCancellableException(Long id, ReservationStatus status) {
        super("Reservation process " + id + " cannot be cancelled in " + status);
    }

    public ReservationNotCancellableException(Long id, Throwable cause) {
        super("Catedra does not allow cancelling reservation process " + id, cause);
    }
}
