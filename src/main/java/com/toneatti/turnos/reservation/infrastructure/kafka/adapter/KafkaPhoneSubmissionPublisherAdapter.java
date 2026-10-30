package com.toneatti.turnos.reservation.infrastructure.kafka.adapter;

import com.toneatti.turnos.config.CatedraIntegrationProperties;
import com.toneatti.turnos.reservation.application.exception.PhoneSubmissionUnavailableException;
import com.toneatti.turnos.reservation.domain.model.PhoneSubmission;
import com.toneatti.turnos.reservation.domain.ports.out.PhoneSubmissionPublisher;
import com.toneatti.turnos.reservation.infrastructure.kafka.mapper.AdditionalInformationSubmittedMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publishes to the phone topic with the reservationProcessId as message key, so the event shares the partition of the
 * other events of the process (section 15.1 of the integration contract). The send is synchronous: the app learns
 * right away whether the cátedra received the phone. Waiting is bounded by {@code sendTimeout}, both to get the
 * metadata of the broker and to get the acknowledgement. The phone number is never logged.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPhoneSubmissionPublisherAdapter implements PhoneSubmissionPublisher {

    private final KafkaTemplate<String, String> phoneSubmissionKafkaTemplate;
    private final AdditionalInformationSubmittedMessageMapper additionalInformationSubmittedMessageMapper;
    private final JsonMapper jsonMapper;
    private final CatedraIntegrationProperties properties;

    @Override
    public void publish(PhoneSubmission submission) {
        String json = jsonMapper.writeValueAsString(
                additionalInformationSubmittedMessageMapper.toMessage(submission, properties.groupId()));
        try {
            phoneSubmissionKafkaTemplate.sendDefault(submission.reservationProcessId(), json)
                    .get(properties.kafka().sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable(submission, e);
        } catch (ExecutionException | TimeoutException | KafkaException
                 | org.springframework.kafka.KafkaException e) {
            throw unavailable(submission, e);
        }
    }

    private static PhoneSubmissionUnavailableException unavailable(PhoneSubmission submission, Exception cause) {
        log.warn("Could not publish the phone event {} of reservation process {}", submission.eventId(),
                submission.reservationProcessId(), cause);
        return new PhoneSubmissionUnavailableException("Could not publish the phone event " + submission.eventId()
                + " of reservation process " + submission.reservationProcessId(), cause);
    }
}
