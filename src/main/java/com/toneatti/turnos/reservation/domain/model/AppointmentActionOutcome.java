package com.toneatti.turnos.reservation.domain.model;

/**
 * What processing an event of the cátedra did. Only APPLIED changed the local process.
 */
public enum AppointmentActionOutcome {
    APPLIED,
    // The eventId was already processed (section 18.1 of the integration contract).
    DUPLICATE,
    // The status of the process does not accept the event; a final status is never reopened (section 18.3).
    LATE,
    // No local process has the reservationProcessId of the event.
    UNKNOWN_PROCESS
}
