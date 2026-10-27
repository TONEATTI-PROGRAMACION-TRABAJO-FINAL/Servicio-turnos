package com.toneatti.turnos.support;

import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
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
