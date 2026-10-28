package com.toneatti.turnos.reservation.infrastructure.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Any event of the topic alumnos.turnos.acciones.{groupId} (sections 15.4 and 15.6 to 15.10 of the integration
 * contract), with the fields the local process keeps. The others (groupId, holdId, requestEventId, message,
 * occurredAt and the data of the slot) and any field added in v1 are ignored (section 15.1).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppointmentActionMessage(
        String eventId,
        String eventType,
        String reservationProcessId,
        Long reservationId,
        String reason,
        Instant expiresAt,
        Integer schemaVersion
) {
}
