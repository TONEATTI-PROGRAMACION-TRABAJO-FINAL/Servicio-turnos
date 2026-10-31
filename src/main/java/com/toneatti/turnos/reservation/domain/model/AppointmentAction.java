package com.toneatti.turnos.reservation.domain.model;

import java.time.Instant;

/**
 * An event that the cátedra publishes about a reservation process (sections 15.4 and 15.6 to 15.10 of the integration
 * contract). {@code eventId} identifies the event, so a duplicate is recognized by it; {@code reservationProcessId} is
 * the process of the cátedra it is about. Only the fields the local process keeps are modeled.
 */
public sealed interface AppointmentAction {

    String eventId();

    String reservationProcessId();

    // Its eventId is the requestEventId of the phone sent back (section 15.5).
    record AdditionalInformationRequested(String eventId, String reservationProcessId, Instant expiresAt)
            implements AppointmentAction {
    }

    // reservationId is the id of the reservation in the cátedra.
    record AppointmentConfirmed(String eventId, String reservationProcessId, Long reservationId)
            implements AppointmentAction {
    }

    // The phone can be sent again until expiresAt.
    record AdditionalInformationRejected(String eventId, String reservationProcessId, String reason,
                                         Instant expiresAt) implements AppointmentAction {
    }

    record AppointmentProcessExpired(String eventId, String reservationProcessId, String reason)
            implements AppointmentAction {
    }

    record AppointmentProcessInvalid(String eventId, String reservationProcessId, String reason)
            implements AppointmentAction {
    }

    // reason may be null.
    record AppointmentCancelled(String eventId, String reservationProcessId, String reason)
            implements AppointmentAction {
    }
}
