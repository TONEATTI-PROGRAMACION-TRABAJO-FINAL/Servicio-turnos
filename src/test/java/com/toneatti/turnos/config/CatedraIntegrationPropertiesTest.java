package com.toneatti.turnos.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.validation.FieldError;

import java.time.Duration;

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
            assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
            assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(5));
            assertThat(properties.kafka().bootstrapServers()).isEqualTo("localhost:9092");
            assertThat(properties.kafka().consumerGroupId()).isEqualTo("alumnos-test");
            assertThat(properties.kafka().appointmentActionsTopic()).isEqualTo("alumnos.turnos.acciones.test");
            assertThat(properties.kafka().appointmentPhoneTopic()).isEqualTo("catedra.turnos.telefono.test");
        });
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

    @Test
    void failsWhenTheConnectTimeoutIsNotPositive() {
        assertFailsOn("connectTimeout", "catedra.connect-timeout=PT0S");
    }

    @Test
    void failsWhenTheReadTimeoutIsNotPositive() {
        assertFailsOn("readTimeout", "catedra.read-timeout=-PT1S");
    }

    @Test
    void failsWhenTheJwtIsMissing() {
        assertFailsOn("jwt", "catedra.jwt=");
    }

    @Test
    void failsWhenTheApiBaseUrlIsMissing() {
        assertFailsOn("apiBaseUrl", "catedra.api-base-url=");
    }

    @Test
    void failsWhenTheApiBaseUrlIsNotAUrl() {
        assertFailsOn("apiBaseUrl", "catedra.api-base-url=not a url");
    }

    @Test
    void failsWhenTheActionsTopicIsMissing() {
        assertFailsOn("kafka.appointmentActionsTopic", "catedra.kafka.appointment-actions-topic=");
    }

    @Test
    void failsWhenThePhoneTopicIsMissing() {
        assertFailsOn("kafka.appointmentPhoneTopic", "catedra.kafka.appointment-phone-topic=");
    }

    @Test
    void toStringMasksTheJwt() {
        contextRunner.run(context -> {
            String text = context.getBean(CatedraIntegrationProperties.class).toString();

            assertThat(text).doesNotContain("test-jwt");
            assertThat(text).contains("http://localhost:8089", "localhost:9092");
        });
    }

    private void assertFailsOn(String field, String override) {
        contextRunner.withPropertyValues(override).run(context -> {
            assertThat(context).hasFailed();
            Throwable rootCause = NestedExceptionUtils.getRootCause(context.getStartupFailure());
            assertThat(rootCause).isInstanceOf(BindValidationException.class);
            assertThat(((BindValidationException) rootCause).getValidationErrors().getAllErrors())
                    .extracting(error -> ((FieldError) error).getField())
                    .containsExactly(field);
        });
    }
}
