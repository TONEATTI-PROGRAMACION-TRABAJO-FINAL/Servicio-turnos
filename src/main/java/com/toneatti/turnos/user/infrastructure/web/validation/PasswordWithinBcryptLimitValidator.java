package com.toneatti.turnos.user.infrastructure.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class PasswordWithinBcryptLimitValidator implements ConstraintValidator<PasswordWithinBcryptLimit, String> {

    // BCrypt rejects passwords longer than 72 bytes, and a non-ASCII character takes more than one byte in UTF-8.
    private static final int BCRYPT_MAX_BYTES = 72;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }
}
