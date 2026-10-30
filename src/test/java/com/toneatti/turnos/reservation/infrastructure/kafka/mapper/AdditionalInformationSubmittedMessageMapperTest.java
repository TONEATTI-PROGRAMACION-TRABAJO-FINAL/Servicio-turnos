package com.toneatti.turnos.reservation.infrastructure.kafka.mapper;

import com.toneatti.turnos.reservation.infrastructure.kafka.dto.AdditionalInformationSubmittedMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.toneatti.turnos.reservation.domain.model.PhoneSubmissionFixtures.SUBMISSION;
import static org.assertj.core.api.Assertions.assertThat;

class AdditionalInformationSubmittedMessageMapperTest {

    private final AdditionalInformationSubmittedMessageMapper additionalInformationSubmittedMessageMapper =
            new AdditionalInformationSubmittedMessageMapper();

    @Test
    void returnsNullForNullInput() {
        assertThat(additionalInformationSubmittedMessageMapper.toMessage(null, "proyecto-test")).isNull();
    }

    @Test
    void mapsEveryFieldOfTheContract() {
        AdditionalInformationSubmittedMessage message =
                additionalInformationSubmittedMessageMapper.toMessage(SUBMISSION, "proyecto-juan-perez");

        assertThat(message).isEqualTo(new AdditionalInformationSubmittedMessage("phone-submitted-b91e",
                "AdditionalInformationSubmitted", "proyecto-juan-perez", "process-a92b47d0", "hold-6c0c2f41",
                "request-event-8a31", new AdditionalInformationSubmittedMessage.Information("+5492615551234"),
                Instant.parse("2099-09-10T14:36:00Z"), 1));
    }

    @Test
    void doesNotExposeThePhoneNumberInItsText() {
        AdditionalInformationSubmittedMessage message =
                additionalInformationSubmittedMessageMapper.toMessage(SUBMISSION, "proyecto-juan-perez");

        assertThat(message.toString()).doesNotContain("5551234");
    }
}
