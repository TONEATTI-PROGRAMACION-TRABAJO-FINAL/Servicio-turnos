package com.toneatti.turnos.reservation.domain.model;

import java.time.Instant;

/**
 * The phone number the user sent for a reservation process that is waiting for it (section 15.5 of the integration
 * contract). {@code eventId} is new for each submission, so a phone sent again after a rejection is another event;
 * {@code requestEventId} is the event of the cátedra that asked for the phone. The phone number is masked in toString.
 */
public record PhoneSubmission(
        String eventId,
        String reservationProcessId,
        String holdId,
        String requestEventId,
        PhoneNumber phoneNumber,
        Instant occurredAt
) {
}
