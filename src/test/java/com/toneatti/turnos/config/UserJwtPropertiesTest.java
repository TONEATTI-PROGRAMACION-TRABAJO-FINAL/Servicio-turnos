package com.toneatti.turnos.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.validation.FieldError;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class UserJwtPropertiesTest {

    private static final String SECRET = "test-secret-with-at-least-32-bytes!";

    @EnableConfigurationProperties(UserJwtProperties.class)
    static class TestConfig {
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues("user.jwt.secret=" + SECRET);

    @Test
    void bindsTheSecretAndDefaultsTheExpirationTo24Hours() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            UserJwtProperties properties = context.getBean(UserJwtProperties.class);

            assertThat(properties.secret()).isEqualTo(SECRET);
            assertThat(properties.expiration()).isEqualTo(Duration.ofHours(24));
        });
    }

    @Test
    void bindsAConfiguredExpiration() {
        contextRunner.withPropertyValues("user.jwt.expiration=PT30M").run(context ->
                assertThat(context.getBean(UserJwtProperties.class).expiration()).isEqualTo(Duration.ofMinutes(30)));
    }

    @Test
    void failsWhenTheSecretIsMissing() {
        assertFailsOn("secret", "user.jwt.secret=");
    }

    @Test
    void failsWhenTheSecretIsShorterThan32Bytes() {
        assertFailsOn("secretLongEnough", "user.jwt.secret=" + "a".repeat(31));
    }

    @Test
    void acceptsASecretOfExactly32Bytes() {
        contextRunner.withPropertyValues("user.jwt.secret=" + "a".repeat(32))
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void failsWhenTheExpirationIsNotPositive() {
        assertFailsOn("expirationPositive", "user.jwt.expiration=PT0S");
    }

    @Test
    void toStringMasksTheSecret() {
        contextRunner.run(context -> {
            String text = context.getBean(UserJwtProperties.class).toString();

            assertThat(text).doesNotContain(SECRET);
            assertThat(text).contains("PT24H");
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
