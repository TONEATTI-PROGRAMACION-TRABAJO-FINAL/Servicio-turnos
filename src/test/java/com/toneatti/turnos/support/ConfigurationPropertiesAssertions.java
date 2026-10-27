package com.toneatti.turnos.support;

import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.validation.FieldError;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assertions for the validation of {@code @ConfigurationProperties} records bound by an {@link ApplicationContextRunner}.
 */
public final class ConfigurationPropertiesAssertions {

    private ConfigurationPropertiesAssertions() {
    }

    /**
     * The runner with those {@code NAME=value} pairs as operating system environment variables, so they go through
     * the same relaxed binding as the real ones (for example CATEDRA_READ_TIMEOUT to catedra.read-timeout).
     */
    public static ApplicationContextRunner withEnvironmentVariables(ApplicationContextRunner contextRunner,
                                                                    String... variables) {
        return contextRunner.withInitializer(context -> TestPropertyValues.of(variables)
                .applyTo(context.getEnvironment(), TestPropertyValues.Type.SYSTEM_ENVIRONMENT));
    }

    /**
     * With the override applied the context fails to start, and the only validation error is on that field.
     */
    public static void assertFailsOn(ApplicationContextRunner contextRunner, String field, String override) {
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
