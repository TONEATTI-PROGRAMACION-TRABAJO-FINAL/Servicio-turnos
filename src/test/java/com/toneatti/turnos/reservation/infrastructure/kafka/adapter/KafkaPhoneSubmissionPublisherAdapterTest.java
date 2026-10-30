package com.toneatti.turnos.reservation.infrastructure.kafka.adapter;

import com.toneatti.turnos.reservation.application.exception.PhoneSubmissionUnavailableException;
import com.toneatti.turnos.reservation.infrastructure.kafka.mapper.AdditionalInformationSubmittedMessageMapper;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.toneatti.turnos.reservation.domain.model.PhoneSubmissionFixtures.SUBMISSION;
import static com.toneatti.turnos.reservation.infrastructure.kafka.config.CatedraKafkaTestProperties.withSecurityProtocol;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaPhoneSubmissionPublisherAdapterTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private CompletableFuture<SendResult<String, String>> sent;

    private KafkaPhoneSubmissionPublisherAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new KafkaPhoneSubmissionPublisherAdapter(kafkaTemplate,
                new AdditionalInformationSubmittedMessageMapper(), jsonMapper, withSecurityProtocol("PLAINTEXT"));
    }

    // Thread.interrupted() also clears the flag, so it does not leak into the next test.
    @AfterEach
    void clearTheInterruption() {
        Thread.interrupted();
    }

    @Test
    void publishesTheEventOfTheContractKeyedByTheReservationProcess() throws Exception {
        when(kafkaTemplate.sendDefault(anyString(), anyString())).thenReturn(sent);

        adapter.publish(SUBMISSION);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).sendDefault(eq("process-a92b47d0"), json.capture());
        assertThat(jsonMapper.readValue(json.getValue(), Map.class)).isEqualTo(Map.of(
                "eventId", "phone-submitted-b91e",
                "eventType", "AdditionalInformationSubmitted",
                "groupId", "proyecto-test",
                "reservationProcessId", "process-a92b47d0",
                "holdId", "hold-6c0c2f41",
                "requestEventId", "request-event-8a31",
                "information", Map.of("phoneNumber", "+5492615551234"),
                "occurredAt", "2099-09-10T14:36:00Z",
                "schemaVersion", 1));
    }

    @Test
    void waitsForTheAcknowledgementAtMostTheSendTimeout() throws Exception {
        when(kafkaTemplate.sendDefault(anyString(), anyString())).thenReturn(sent);

        adapter.publish(SUBMISSION);

        verify(sent).get(3_000, TimeUnit.MILLISECONDS);
    }

    @Test
    void failsWhenTheAcknowledgementDoesNotArriveInTime() throws Exception {
        when(kafkaTemplate.sendDefault(anyString(), anyString())).thenReturn(sent);
        when(sent.get(anyLong(), eq(TimeUnit.MILLISECONDS)))
                .thenThrow(new java.util.concurrent.TimeoutException());

        assertThatThrownBy(() -> adapter.publish(SUBMISSION))
                .isInstanceOf(PhoneSubmissionUnavailableException.class)
                .hasMessageNotContaining("5551234");
    }

    @Test
    void failsWhenTheBrokerRejectsTheEvent() {
        when(kafkaTemplate.sendDefault(anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException("Expiring 1 record")));

        assertThatThrownBy(() -> adapter.publish(SUBMISSION))
                .isInstanceOf(PhoneSubmissionUnavailableException.class)
                .hasRootCauseInstanceOf(TimeoutException.class);
    }

    // For example, the metadata of the broker did not arrive within max.block.ms.
    @Test
    void failsWhenTheSendFailsRightAway() {
        when(kafkaTemplate.sendDefault(anyString(), anyString()))
                .thenThrow(new org.springframework.kafka.KafkaException("Send failed", new TimeoutException("")));

        assertThatThrownBy(() -> adapter.publish(SUBMISSION))
                .isInstanceOf(PhoneSubmissionUnavailableException.class);
    }

    @Test
    void keepsTheInterruptionOfTheThread() throws Exception {
        when(kafkaTemplate.sendDefault(anyString(), anyString())).thenReturn(sent);
        when(sent.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenThrow(new InterruptedException());

        assertThatThrownBy(() -> adapter.publish(SUBMISSION))
                .isInstanceOf(PhoneSubmissionUnavailableException.class);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }
}
