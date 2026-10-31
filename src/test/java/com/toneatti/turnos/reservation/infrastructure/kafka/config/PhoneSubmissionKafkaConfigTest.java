package com.toneatti.turnos.reservation.infrastructure.kafka.config;

import com.toneatti.turnos.config.CatedraIntegrationProperties;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import static com.toneatti.turnos.reservation.infrastructure.kafka.config.CatedraKafkaTestProperties.withSecurityProtocol;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class PhoneSubmissionKafkaConfigTest {

    private static final CatedraIntegrationProperties PROPERTIES = withSecurityProtocol("SASL_SSL");

    private final PhoneSubmissionKafkaConfig config = new PhoneSubmissionKafkaConfig();

    @Test
    void sendsTextToTheConfiguredConnection() {
        ProducerFactory<String, String> producerFactory = config.phoneSubmissionProducerFactory(PROPERTIES);

        assertThat(producerFactory.getConfigurationProperties()).contains(
                entry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092"),
                entry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class),
                entry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class),
                entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL"));
    }

    // Acknowledged by every in-sync replica, and retried without duplicating the event in the topic.
    @Test
    void waitsForEveryReplicaWithAnIdempotentProducer() {
        ProducerFactory<String, String> producerFactory = config.phoneSubmissionProducerFactory(PROPERTIES);

        assertThat(producerFactory.getConfigurationProperties()).contains(
                entry(ProducerConfig.ACKS_CONFIG, "all"),
                entry(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true));
    }

    @Test
    void boundsTheWholeSendWithTheSendTimeout() {
        ProducerFactory<String, String> producerFactory = config.phoneSubmissionProducerFactory(PROPERTIES);

        assertThat(producerFactory.getConfigurationProperties()).contains(
                entry(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3_000L),
                entry(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 3_000),
                entry(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 3_000),
                entry(ProducerConfig.LINGER_MS_CONFIG, 0));
    }

    // Kafka validates the configuration (types, delivery.timeout.ms >= linger.ms + request.timeout.ms) only when the
    // producer is created, which does not connect to the broker yet.
    @Test
    void createsAProducerKafkaAccepts() {
        ProducerFactory<String, String> producerFactory =
                config.phoneSubmissionProducerFactory(withSecurityProtocol("PLAINTEXT"));

        try (Producer<String, String> producer = producerFactory.createProducer()) {
            assertThat(producer).isNotNull();
        }
    }

    @Test
    void sendsToThePhoneTopicByDefault() {
        KafkaTemplate<String, String> template = config.phoneSubmissionKafkaTemplate(
                config.phoneSubmissionProducerFactory(PROPERTIES), PROPERTIES);

        assertThat(template.getDefaultTopic()).isEqualTo("catedra.turnos.telefono.test");
    }
}
