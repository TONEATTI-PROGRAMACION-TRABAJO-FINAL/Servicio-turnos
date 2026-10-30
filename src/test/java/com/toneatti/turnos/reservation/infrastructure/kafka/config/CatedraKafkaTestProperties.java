package com.toneatti.turnos.reservation.infrastructure.kafka.config;

import com.toneatti.turnos.config.CatedraIntegrationProperties;

import java.time.Duration;

/**
 * Properties with values different from the defaults, so the Kafka configurations are seen taking them. The broker
 * is on localhost because Kafka resolves it when a producer is created, without connecting yet.
 */
final class CatedraKafkaTestProperties {

    static final Duration SEND_TIMEOUT = Duration.ofSeconds(3);

    private CatedraKafkaTestProperties() {
    }

    static CatedraIntegrationProperties withSecurityProtocol(String securityProtocol) {
        return new CatedraIntegrationProperties("http://localhost:8089", "test-jwt", "proyecto-test",
                Duration.ofSeconds(2), Duration.ofSeconds(5),
                new CatedraIntegrationProperties.Kafka("localhost:19092", "alumnos-test",
                        "alumnos.turnos.acciones.test", "catedra.turnos.telefono.test", securityProtocol, "latest",
                        Duration.ofMinutes(2), SEND_TIMEOUT, false));
    }
}
