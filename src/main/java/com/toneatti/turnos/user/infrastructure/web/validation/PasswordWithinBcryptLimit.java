package com.toneatti.turnos.user.infrastructure.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The password fits in the 72 bytes BCrypt can process. A null value is valid: {@code @NotBlank} handles it.
 */
@Documented
@Constraint(validatedBy = PasswordWithinBcryptLimitValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordWithinBcryptLimit {

    String message() default "Password must have at most 72 bytes in UTF-8";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
