package com.toneatti.turnos.reservation.infrastructure.kafka.config;

import com.toneatti.turnos.config.CatedraIntegrationProperties;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

/**
 * Producer of the phone topic (section 15.5 of the integration contract), configured from
 * {@link CatedraIntegrationProperties} like the consumer of the appointment actions. The value is sent as text, already
 * serialized. A send is acknowledged by every in-sync replica, and the idempotent producer retries it without
 * duplicating it in the topic. {@code sendTimeout} bounds the whole send: waiting for the metadata of the broker
 * (max.block.ms, 60 seconds by default) and the acknowledgement with its retries (delivery.timeout.ms), so a broker that
 * is down does not hold the request of the app.
 * The template replaces the one of Spring Boot and sends to the phone topic by default.
 */
@Configuration
public class PhoneSubmissionKafkaConfig {

    @Bean
    public ProducerFactory<String, String> phoneSubmissionProducerFactory(CatedraIntegrationProperties properties) {
        CatedraIntegrationProperties.Kafka kafka = properties.kafka();
        int sendTimeoutMillis = Math.toIntExact(kafka.sendTimeout().toMillis());
        return new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
                // delivery.timeout.ms must be at least linger.ms + request.timeout.ms.
                ProducerConfig.LINGER_MS_CONFIG, 0,
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, sendTimeoutMillis,
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, sendTimeoutMillis,
                ProducerConfig.MAX_BLOCK_MS_CONFIG, kafka.sendTimeout().toMillis(),
                CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, kafka.securityProtocol()));
    }

    @Bean
    public KafkaTemplate<String, String> phoneSubmissionKafkaTemplate(
            ProducerFactory<String, String> phoneSubmissionProducerFactory,
            CatedraIntegrationProperties properties) {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(phoneSubmissionProducerFactory);
        template.setDefaultTopic(properties.kafka().appointmentPhoneTopic());
        return template;
    }
}
