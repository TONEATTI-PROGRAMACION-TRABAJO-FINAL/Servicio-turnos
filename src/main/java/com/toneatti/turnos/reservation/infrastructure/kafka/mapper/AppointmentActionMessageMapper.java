package com.toneatti.turnos.reservation.infrastructure.kafka.mapper;

import com.toneatti.turnos.reservation.domain.model.AppointmentAction;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction.AdditionalInformationRejected;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction.AdditionalInformationRequested;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction.AppointmentCancelled;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction.AppointmentConfirmed;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction.AppointmentProcessExpired;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction.AppointmentProcessInvalid;
import com.toneatti.turnos.reservation.infrastructure.kafka.dto.AppointmentActionMessage;
import org.springframework.stereotype.Component;

/**
 * Throws IllegalArgumentException for a message that is not a v1 event of the contract: without its eventId,
 * eventType or reservationProcessId it cannot be deduplicated nor applied, and an unknown eventType or schemaVersion
 * has no meaning in v1.
 */
@Component
public class AppointmentActionMessageMapper {

    private static final int SCHEMA_VERSION = 1;

    public AppointmentAction toDomain(AppointmentActionMessage message) {
        if (message == null) {
            return null;
        }
        String eventId = required(message.eventId(), "eventId");
        String processId = required(message.reservationProcessId(), "reservationProcessId");
        if (message.schemaVersion() == null || message.schemaVersion() != SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported schemaVersion " + message.schemaVersion());
        }
        return switch (required(message.eventType(), "eventType")) {
            case "AdditionalInformationRequested" ->
                    new AdditionalInformationRequested(eventId, processId, message.expiresAt());
            case "AppointmentConfirmed" -> new AppointmentConfirmed(eventId, processId, message.reservationId());
            case "AdditionalInformationRejected" ->
                    new AdditionalInformationRejected(eventId, processId, message.reason(), message.expiresAt());
            case "AppointmentProcessExpired" -> new AppointmentProcessExpired(eventId, processId, message.reason());
            case "AppointmentProcessInvalid" -> new AppointmentProcessInvalid(eventId, processId, message.reason());
            case "AppointmentCancelled" -> new AppointmentCancelled(eventId, processId, message.reason());
            default -> throw new IllegalArgumentException("Unknown eventType " + message.eventType());
        };
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing " + field);
        }
        return value;
    }
}
