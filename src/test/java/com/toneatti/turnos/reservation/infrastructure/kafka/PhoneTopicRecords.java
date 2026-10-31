package com.toneatti.turnos.reservation.infrastructure.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static com.toneatti.turnos.reservation.infrastructure.kafka.config.CatedraKafkaTestProperties.PHONE_TOPIC;

/**
 * Reads the phone topic of an embedded broker from its beginning, with a consumer group of its own.
 */
public final class PhoneTopicRecords {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private PhoneTopicRecords() {
    }

    /**
     * The first {@code count} records of the topic with that key, in their order; waits until they are there.
     */
    public static List<ConsumerRecord<String, String>> recordsWithKey(EmbeddedKafkaBroker broker, String key,
                                                                    int count) {
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                KafkaTestUtils.consumerProps(broker, "test-" + UUID.randomUUID(), false),
                new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, PHONE_TOPIC);
            return StreamSupport.stream(KafkaTestUtils.getRecords(consumer, TIMEOUT, count).spliterator(), false)
                    .filter(consumerRecord -> key.equals(consumerRecord.key()))
                    .toList();
        }
    }
}
