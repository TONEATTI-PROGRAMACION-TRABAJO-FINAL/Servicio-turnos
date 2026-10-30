package com.toneatti.turnos.reservation.infrastructure.kafka.config;

import com.toneatti.turnos.config.CatedraIntegrationProperties;

import java.time.Duration;

/**
 * Properties with values different from the defaults, so the Kafka configurations are seen taking them. The broker
 * is on localhost because Kafka resolves it when a producer is created, without connecting yet.
 */
public final class CatedraKafkaTestProperties {

    public static final String GROUP_ID = "proyecto-test";
    public static final String PHONE_TOPIC = "catedra.turnos.telefono.test";
    public static final Duration SEND_TIMEOUT = Duration.ofSeconds(3);

    private CatedraKafkaTestProperties() {
    }

    public static CatedraIntegrationProperties withSecurityProtocol(String securityProtocol) {
        return properties("localhost:19092", securityProtocol, SEND_TIMEOUT);
    }

    public static CatedraIntegrationProperties withBroker(String bootstrapServers, Duration sendTimeout) {
        return properties(bootstrapServers, "PLAINTEXT", sendTimeout);
    }

    private static CatedraIntegrationProperties properties(String bootstrapServers, String securityProtocol,
                                                           Duration sendTimeout) {
        return new CatedraIntegrationProperties("http://localhost:8089", "test-jwt", GROUP_ID,
                Duration.ofSeconds(2), Duration.ofSeconds(5),
                new CatedraIntegrationProperties.Kafka(bootstrapServers, "alumnos-test",
                        "alumnos.turnos.acciones.test", PHONE_TOPIC, securityProtocol, "latest",
                        Duration.ofMinutes(2), sendTimeout, false));
    }
}
