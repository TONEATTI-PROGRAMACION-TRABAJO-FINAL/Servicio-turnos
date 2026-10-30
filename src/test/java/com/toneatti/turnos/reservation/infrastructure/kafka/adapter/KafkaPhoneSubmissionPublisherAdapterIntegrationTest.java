package com.toneatti.turnos.reservation.infrastructure.kafka.adapter;

import com.toneatti.turnos.reservation.application.exception.PhoneSubmissionUnavailableException;
import com.toneatti.turnos.reservation.domain.ports.out.PhoneSubmissionPublisher;
import com.toneatti.turnos.reservation.infrastructure.kafka.config.PhoneSubmissionKafkaConfig;
import com.toneatti.turnos.reservation.infrastructure.kafka.mapper.AdditionalInformationSubmittedMessageMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static com.toneatti.turnos.reservation.domain.model.PhoneSubmissionFixtures.submission;
import static com.toneatti.turnos.reservation.infrastructure.kafka.config.CatedraKafkaTestProperties.withBroker;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The producer against an embedded broker, never the environment of the cátedra, with the beans and the properties
 * of the application. Each test uses its own reservationProcessId, so it only reads its own events.
 */
@SpringBootTest(properties = "catedra.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 1, topics = KafkaPhoneSubmissionPublisherAdapterIntegrationTest.TOPIC)
@ActiveProfiles("test")
class KafkaPhoneSubmissionPublisherAdapterIntegrationTest {

    // From application-test.properties.
    static final String TOPIC = "catedra.turnos.telefono.test";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private PhoneSubmissionPublisher phoneSubmissionPublisher;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void publishesTheEventOfTheContractKeyedByTheReservationProcess() {
        String processId = "process-" + UUID.randomUUID();

        phoneSubmissionPublisher.publish(submission(processId));

        List<ConsumerRecord<String, String>> records = recordsOf(processId);
        assertThat(records).hasSize(1);
        assertThat(jsonMapper.readValue(records.getFirst().value(), Map.class)).isEqualTo(Map.of(
                "eventId", "phone-submitted-b91e",
                "eventType", "AdditionalInformationSubmitted",
                "groupId", "proyecto-test",
                "reservationProcessId", processId,
                "holdId", "hold-6c0c2f41",
                "requestEventId", "request-event-8a31",
                "information", Map.of("phoneNumber", "+5492615551234"),
                "occurredAt", "2099-09-10T14:36:00Z",
                "schemaVersion", 1));
    }

    // Nothing listens on port 1: the producer never gets the metadata of the broker and gives up after sendTimeout
    // instead of the 60 seconds of max.block.ms by default.
    @Test
    void failsWithinTheSendTimeoutWhenTheBrokerIsDown() {
        PhoneSubmissionKafkaConfig config = new PhoneSubmissionKafkaConfig();
        var properties = withBroker("localhost:1", Duration.ofSeconds(1));
        KafkaTemplate<String, String> template = config.phoneSubmissionKafkaTemplate(
                config.phoneSubmissionProducerFactory(properties), properties);
        PhoneSubmissionPublisher publisher = new KafkaPhoneSubmissionPublisherAdapter(template,
                new AdditionalInformationSubmittedMessageMapper(), jsonMapper, properties);
        long start = System.nanoTime();

        try {
            assertThatThrownBy(() -> publisher.publish(submission("process-" + UUID.randomUUID())))
                    .isInstanceOf(PhoneSubmissionUnavailableException.class);
        } finally {
            template.destroy();
        }
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(TIMEOUT);
    }

    private List<ConsumerRecord<String, String>> recordsOf(String processId) {
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                KafkaTestUtils.consumerProps(broker, "test-" + UUID.randomUUID(), false),
                new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, TOPIC);
            return StreamSupport.stream(KafkaTestUtils.getRecords(consumer, TIMEOUT).spliterator(), false)
                    .filter(consumerRecord -> processId.equals(consumerRecord.key()))
                    .toList();
        }
    }
}
