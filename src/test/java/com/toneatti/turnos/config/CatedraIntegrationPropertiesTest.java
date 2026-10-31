package com.toneatti.turnos.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static com.toneatti.turnos.support.ConfigurationPropertiesAssertions.assertFailsOn;
import static com.toneatti.turnos.support.ConfigurationPropertiesAssertions.withEnvironmentVariables;
import static org.assertj.core.api.Assertions.assertThat;

class CatedraIntegrationPropertiesTest {

    @EnableConfigurationProperties(CatedraIntegrationProperties.class)
    static class TestConfig {
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues(
                    "catedra.api-base-url=http://localhost:8089",
                    "catedra.jwt=test-jwt",
                    "catedra.group-id=proyecto-test",
                    "catedra.kafka.bootstrap-servers=localhost:9092",
                    "catedra.kafka.consumer-group-id=alumnos-test",
                    "catedra.kafka.appointment-actions-topic=alumnos.turnos.acciones.test",
                    "catedra.kafka.appointment-phone-topic=catedra.turnos.telefono.test");

    @Test
    void bindsAllPropertiesWhenTheyArePresent() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            CatedraIntegrationProperties properties = context.getBean(CatedraIntegrationProperties.class);

            assertThat(properties.apiBaseUrl()).isEqualTo("http://localhost:8089");
            assertThat(properties.jwt()).isEqualTo("test-jwt");
            assertThat(properties.groupId()).isEqualTo("proyecto-test");
            assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
            assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(5));
            assertThat(properties.kafka().bootstrapServers()).isEqualTo("localhost:9092");
            assertThat(properties.kafka().consumerGroupId()).isEqualTo("alumnos-test");
            assertThat(properties.kafka().appointmentActionsTopic()).isEqualTo("alumnos.turnos.acciones.test");
            assertThat(properties.kafka().appointmentPhoneTopic()).isEqualTo("catedra.turnos.telefono.test");
            assertThat(properties.kafka().securityProtocol()).isEqualTo("PLAINTEXT");
            assertThat(properties.kafka().autoOffsetReset()).isEqualTo("earliest");
            assertThat(properties.kafka().retryMaxElapsedTime()).isEqualTo(Duration.ofMinutes(2));
            assertThat(properties.kafka().sendTimeout()).isEqualTo(Duration.ofSeconds(5));
            assertThat(properties.kafka().listenerEnabled()).isTrue();
        });
    }

    // The variables documented in application.properties and .env.example.
    @Test
    void bindsTheKafkaConsumerSettingsFromEnvironmentVariables() {
        withEnvironmentVariables(contextRunner, "CATEDRA_KAFKA_SECURITY_PROTOCOL=SASL_SSL",
                "CATEDRA_KAFKA_AUTO_OFFSET_RESET=latest", "CATEDRA_KAFKA_RETRY_MAX_ELAPSED_TIME=PT30S",
                "CATEDRA_KAFKA_LISTENER_ENABLED=false")
                .run(context -> {
                    CatedraIntegrationProperties.Kafka kafka =
                            context.getBean(CatedraIntegrationProperties.class).kafka();

                    assertThat(kafka.securityProtocol()).isEqualTo("SASL_SSL");
                    assertThat(kafka.autoOffsetReset()).isEqualTo("latest");
                    assertThat(kafka.retryMaxElapsedTime()).isEqualTo(Duration.ofSeconds(30));
                    assertThat(kafka.listenerEnabled()).isFalse();
                });
    }

    // The variables documented in application.properties and .env.example.
    @Test
    void bindsTheGroupIdAndTheSendTimeoutFromEnvironmentVariables() {
        withEnvironmentVariables(contextRunner, "CATEDRA_GROUP_ID=proyecto-juan-perez",
                "CATEDRA_KAFKA_SEND_TIMEOUT=PT1.5S")
                .run(context -> {
                    CatedraIntegrationProperties properties = context.getBean(CatedraIntegrationProperties.class);

                    assertThat(properties.groupId()).isEqualTo("proyecto-juan-perez");
                    assertThat(properties.kafka().sendTimeout()).isEqualTo(Duration.ofMillis(1_500));
                });
    }

    @Test
    void failsWhenTheGroupIdIsMissing() {
        assertFailsOn(contextRunner, "groupId", "catedra.group-id=");
    }

    @Test
    void failsWhenTheSendTimeoutIsNotPositive() {
        assertFailsOn(contextRunner, "kafka.sendTimeout", "catedra.kafka.send-timeout=PT0S");
    }

    @Test
    void failsWhenTheSecurityProtocolIsUnknown() {
        assertFailsOn(contextRunner, "kafka.securityProtocol", "catedra.kafka.security-protocol=TLS");
    }

    @Test
    void failsWhenTheAutoOffsetResetIsNotEarliestNorLatest() {
        assertFailsOn(contextRunner, "kafka.autoOffsetReset", "catedra.kafka.auto-offset-reset=none");
    }

    @Test
    void failsWhenTheRetryTimeIsNotPositive() {
        assertFailsOn(contextRunner, "kafka.retryMaxElapsedTime", "catedra.kafka.retry-max-elapsed-time=PT0S");
    }

    @Test
    void bindsConfiguredTimeouts() {
        contextRunner.withPropertyValues("catedra.connect-timeout=PT1S", "catedra.read-timeout=PT0.5S")
                .run(context -> {
                    CatedraIntegrationProperties properties = context.getBean(CatedraIntegrationProperties.class);

                    assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(properties.readTimeout()).isEqualTo(Duration.ofMillis(500));
                });
    }

    // The variables documented in application.properties and .env.example.
    @Test
    void bindsTheTimeoutsFromEnvironmentVariables() {
        withEnvironmentVariables(contextRunner, "CATEDRA_CONNECT_TIMEOUT=PT1S", "CATEDRA_READ_TIMEOUT=PT0.5S")
                .run(context -> {
                    CatedraIntegrationProperties properties = context.getBean(CatedraIntegrationProperties.class);

                    assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(properties.readTimeout()).isEqualTo(Duration.ofMillis(500));
                });
    }

    @Test
    void failsWhenTheConnectTimeoutIsNotPositive() {
        assertFailsOn(contextRunner, "connectTimeout", "catedra.connect-timeout=PT0S");
    }

    @Test
    void failsWhenTheReadTimeoutIsNotPositive() {
        assertFailsOn(contextRunner, "readTimeout", "catedra.read-timeout=-PT1S");
    }

    @Test
    void failsWhenTheJwtIsMissing() {
        assertFailsOn(contextRunner, "jwt", "catedra.jwt=");
    }

    @Test
    void failsWhenTheApiBaseUrlIsMissing() {
        assertFailsOn(contextRunner, "apiBaseUrl", "catedra.api-base-url=");
    }

    @Test
    void failsWhenTheApiBaseUrlIsNotAUrl() {
        assertFailsOn(contextRunner, "apiBaseUrl", "catedra.api-base-url=not a url");
    }

    @Test
    void failsWhenTheActionsTopicIsMissing() {
        assertFailsOn(contextRunner, "kafka.appointmentActionsTopic", "catedra.kafka.appointment-actions-topic=");
    }

    @Test
    void failsWhenThePhoneTopicIsMissing() {
        assertFailsOn(contextRunner, "kafka.appointmentPhoneTopic", "catedra.kafka.appointment-phone-topic=");
    }

    @Test
    void toStringMasksTheJwt() {
        contextRunner.run(context -> {
            String text = context.getBean(CatedraIntegrationProperties.class).toString();

            assertThat(text).doesNotContain("test-jwt");
            assertThat(text).contains("http://localhost:8089", "localhost:9092");
        });
    }
}
