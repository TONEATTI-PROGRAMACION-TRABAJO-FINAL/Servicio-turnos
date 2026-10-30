package com.toneatti.turnos.reservation.infrastructure.kafka.config;

import com.toneatti.turnos.config.CatedraIntegrationProperties;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class AppointmentActionsKafkaConfigTest {

    private static final CatedraIntegrationProperties PROPERTIES = new CatedraIntegrationProperties(
            "http://localhost:8089", "test-jwt", Duration.ofSeconds(2), Duration.ofSeconds(5),
            new CatedraIntegrationProperties.Kafka("kafka.catedra.example.com:9092", "alumnos-test",
                    "alumnos.turnos.acciones.test", "catedra.turnos.telefono.test", "SASL_SSL", "latest",
                    Duration.ofMinutes(2), false));

    private final AppointmentActionsKafkaConfig config = new AppointmentActionsKafkaConfig();

    // Offsets are committed by the container after processing, never automatically in the background.
    @Test
    void consumesTextFromTheConfiguredConnectionWithoutAutoCommit() {
        ConsumerFactory<String, String> consumerFactory = config.appointmentActionsConsumerFactory(PROPERTIES);

        assertThat(consumerFactory.getConfigurationProperties()).contains(
                entry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka.catedra.example.com:9092"),
                entry(ConsumerConfig.GROUP_ID_CONFIG, "alumnos-test"),
                entry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"),
                entry(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false),
                entry(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class),
                entry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class),
                entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL"));
    }

    @Test
    void commitsTheOffsetOfEachRecordAfterProcessingIt() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = config.kafkaListenerContainerFactory(
                config.appointmentActionsConsumerFactory(PROPERTIES), PROPERTIES);

        assertThat(factory.getContainerProperties().getAckMode()).isEqualTo(ContainerProperties.AckMode.RECORD);
    }
}
