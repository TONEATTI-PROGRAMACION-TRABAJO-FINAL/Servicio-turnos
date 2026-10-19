package com.toneatti.turnos.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.validation.FieldError;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogoServicePropertiesTest {

    @EnableConfigurationProperties(CatalogoServiceProperties.class)
    static class TestConfig {
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues("catalogo.base-url=http://localhost:8080");

    @Test
    void bindsTheBaseUrlWhenItIsValid() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(CatalogoServiceProperties.class).baseUrl())
                    .isEqualTo("http://localhost:8080");
        });
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
