package com.toneatti.turnos.reservation.infrastructure.kafka.dto;

import java.time.Instant;

/**
 * The event of the topic catedra.turnos.telefono.{groupId} (section 15.5 of the integration contract). Every field
 * is required.
 */
public record AdditionalInformationSubmittedMessage(
        String eventId,
        String eventType,
        String groupId,
        String reservationProcessId,
        String holdId,
        String requestEventId,
        Information information,
        Instant occurredAt,
        int schemaVersion
) {

    public record Information(String phoneNumber) {

        // Personal data: the generated toString would expose it in logs.
        @Override
        public String toString() {
            return "Information[phoneNumber=****]";
        }
    }
}
