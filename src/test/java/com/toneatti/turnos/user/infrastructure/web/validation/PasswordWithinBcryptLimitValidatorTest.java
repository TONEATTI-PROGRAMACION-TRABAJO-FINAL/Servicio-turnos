package com.toneatti.turnos.user.infrastructure.web.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordWithinBcryptLimitValidatorTest {

    private final PasswordWithinBcryptLimitValidator validator = new PasswordWithinBcryptLimitValidator();

    @Test
    void acceptsNullBecauseNotBlankHandlesIt() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void acceptsExactly72BytesInUtf8() {
        // "ñ" takes 2 bytes in UTF-8: 36 characters are 72 bytes.
        assertThat(validator.isValid("ñ".repeat(36), null)).isTrue();
    }

    @Test
    void rejectsMoreThan72BytesInUtf8() {
        // 37 characters, but 74 bytes in UTF-8.
        assertThat(validator.isValid("ñ".repeat(37), null)).isFalse();
    }
}
