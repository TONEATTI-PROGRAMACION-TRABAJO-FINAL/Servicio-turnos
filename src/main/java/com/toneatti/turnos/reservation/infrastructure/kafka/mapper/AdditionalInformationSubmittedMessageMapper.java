package com.toneatti.turnos.reservation.infrastructure.kafka.mapper;

import com.toneatti.turnos.reservation.domain.model.PhoneSubmission;
import com.toneatti.turnos.reservation.infrastructure.kafka.dto.AdditionalInformationSubmittedMessage;
import org.springframework.stereotype.Component;

/**
 * {@code eventType} and {@code schemaVersion} take the only values the contract accepts; {@code groupId} is the one
 * of the technical integration.
 */
@Component
public class AdditionalInformationSubmittedMessageMapper {

    static final String EVENT_TYPE = "AdditionalInformationSubmitted";
    static final int SCHEMA_VERSION = 1;

    public AdditionalInformationSubmittedMessage toMessage(PhoneSubmission submission, String groupId) {
        if (submission == null) {
            return null;
        }
        return new AdditionalInformationSubmittedMessage(submission.eventId(), EVENT_TYPE, groupId,
                submission.reservationProcessId(), submission.holdId(), submission.requestEventId(),
                new AdditionalInformationSubmittedMessage.Information(submission.phoneNumber().value()),
                submission.occurredAt(), SCHEMA_VERSION);
    }
}
