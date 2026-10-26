package com.toneatti.turnos.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.validation.FieldError;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogoServicePropertiesTest {

    @EnableConfigurationProperties(CatalogoServiceProperties.class)
    static class TestConfig {
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues("catalogo.base-url=http://localhost:8081");

    @Test
    void bindsTheBaseUrlAndDefaultsTheTimeouts() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            CatalogoServiceProperties properties = context.getBean(CatalogoServiceProperties.class);

            assertThat(properties.baseUrl()).isEqualTo("http://localhost:8081");
            assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
            assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(3));
        });
    }

    @Test
    void bindsConfiguredTimeouts() {
        contextRunner.withPropertyValues("catalogo.connect-timeout=PT1S", "catalogo.read-timeout=PT0.5S")
                .run(context -> {
                    CatalogoServiceProperties properties = context.getBean(CatalogoServiceProperties.class);

                    assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(properties.readTimeout()).isEqualTo(Duration.ofMillis(500));
                });
    }

    @Test
    void failsWhenTheConnectTimeoutIsNotPositive() {
        assertFailsOn("connectTimeout", "catalogo.connect-timeout=PT0S");
    }

    @Test
    void failsWhenTheReadTimeoutIsNotPositive() {
        assertFailsOn("readTimeout", "catalogo.read-timeout=-PT1S");
    }

    @Test
    void failsWhenTheCatalogoBaseUrlIsMissing() {
        assertFailsOn("baseUrl", "catalogo.base-url=");
    }

    @Test
    void failsWhenTheCatalogoBaseUrlIsNotAUrl() {
        assertFailsOn("baseUrl", "catalogo.base-url=not a url");
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
