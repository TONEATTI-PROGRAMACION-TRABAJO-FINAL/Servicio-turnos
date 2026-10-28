package com.toneatti.turnos.reservation.domain.model;

import com.toneatti.turnos.reservation.domain.model.AppointmentAction.*;

import java.time.Instant;
import java.util.List;

/**
 * The examples of sections 15.4 and 15.6 to 15.10 of the integration contract, moved to 2099.
 */
public final class AppointmentActionFixtures {

    public static final String PROCESS_ID = "process-a92b47d0";

    public static final AdditionalInformationRequested REQUESTED = new AdditionalInformationRequested(
            "request-event-8a31", PROCESS_ID, Instant.parse("2099-09-10T14:45:50Z"));
    public static final AppointmentConfirmed CONFIRMATION = new AppointmentConfirmed(
            "confirmation-event-7d10", PROCESS_ID, 8251L);
    public static final AdditionalInformationRejected REJECTION = new AdditionalInformationRejected(
            "rejection-event-7d11", PROCESS_ID, "INVALID_PHONE_NUMBER", Instant.parse("2099-09-10T14:46:50Z"));
    public static final AppointmentProcessExpired EXPIRATION = new AppointmentProcessExpired(
            "process-event-7d12", PROCESS_ID, "HOLD_EXPIRED");
    public static final AppointmentProcessInvalid INVALIDATION = new AppointmentProcessInvalid(
            "process-event-7d13", PROCESS_ID, "REQUEST_EVENT_MISMATCH");
    public static final AppointmentCancelled CANCELLATION = new AppointmentCancelled(
            "cancellation-event-7d14", PROCESS_ID, "Cambio solicitado por el usuario");
    public static final List<AppointmentAction> ALL = List.of(REQUESTED, CONFIRMATION, REJECTION, EXPIRATION,
            INVALIDATION, CANCELLATION);

    private AppointmentActionFixtures() {
    }
}
