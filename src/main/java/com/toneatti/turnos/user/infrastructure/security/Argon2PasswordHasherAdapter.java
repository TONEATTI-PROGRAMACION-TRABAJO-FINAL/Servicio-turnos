package com.toneatti.turnos.user.infrastructure.security;

import com.toneatti.turnos.user.domain.ports.out.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Delegates to the Argon2id {@link PasswordEncoder} defined in {@code config.SecurityConfig}.
 */
@Component
@RequiredArgsConstructor
public class Argon2PasswordHasherAdapter implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
