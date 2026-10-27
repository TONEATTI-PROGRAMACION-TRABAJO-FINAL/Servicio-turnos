package com.toneatti.turnos.reservation.domain.model;

/**
 * The reservation process after starting it; {@code resumed} is true when the user already had an active process
 * for the slot and that one was continued instead of creating another.
 */
public record StartedReservation(
        ReservationProcess process,
        boolean resumed
) {
}
