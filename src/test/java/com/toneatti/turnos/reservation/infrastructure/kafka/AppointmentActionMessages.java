package com.toneatti.turnos.reservation.infrastructure.kafka;

import com.toneatti.turnos.reservation.domain.model.AppointmentAction;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction.*;

/**
 * The JSON the cátedra publishes for an event (sections 15.4 and 15.6 to 15.10 of the integration contract), with
 * every field of the contract and one it does not have yet, which the consumer must ignore (section 15.1).
 */
public final class AppointmentActionMessages {

    private static final String SLOT = "\"professionalId\":15,\"date\":\"2099-09-14\",\"startTime\":\"09:30\"";

    private AppointmentActionMessages() {
    }

    public static String json(AppointmentAction action) {
        String fieldsOfTheType = switch (action) {
            case AdditionalInformationRequested requested -> """
                    "requiredField":"PHONE_NUMBER","message":"Ingrese un numero de telefono para continuar.",
                    "expiresAt":"%s\"""".formatted(requested.expiresAt());
            case AppointmentConfirmed confirmed -> """
                    "requestEventId":"request-event-8a31","reservationId":%d,%s,"status":"CONFIRMED\""""
                    .formatted(confirmed.reservationId(), SLOT);
            case AdditionalInformationRejected rejected -> """
                    "requestEventId":"request-event-8a31","reason":"%s","message":"El telefono ingresado no es valido.",
                    "expiresAt":"%s\"""".formatted(rejected.reason(), rejected.expiresAt());
            case AppointmentProcessExpired expired -> reasonOf(expired.reason());
            case AppointmentProcessInvalid invalid -> reasonOf(invalid.reason());
            case AppointmentCancelled cancelled -> """
                    "requestEventId":"request-event-8a31","reservationId":8251,%s,"status":"CANCELLED",%s"""
                    .formatted(SLOT, cancelled.reason() == null ? "\"reason\":null" : reasonOf(cancelled.reason()));
        };
        return """
                {"eventId":"%s","eventType":"%s","groupId":"proyecto-test","reservationProcessId":"%s",
                 "holdId":"hold-6c0c2f41",%s,"occurredAt":"2099-09-10T14:36:01Z","schemaVersion":1,
                 "fieldAddedLater":{"nested":true}}
                """.formatted(action.eventId(), action.getClass().getSimpleName(), action.reservationProcessId(),
                fieldsOfTheType);
    }

    private static String reasonOf(String reason) {
        return "\"reason\":\"" + reason + "\"";
    }
}
