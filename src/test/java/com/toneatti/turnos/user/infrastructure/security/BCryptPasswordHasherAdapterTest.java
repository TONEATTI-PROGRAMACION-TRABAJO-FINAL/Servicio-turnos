package com.toneatti.turnos.user.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherAdapterTest {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final BCryptPasswordHasherAdapter passwordHasher = new BCryptPasswordHasherAdapter(passwordEncoder);

    @Test
    void hashIsNotThePasswordAndMatchesIt() {
        String hash = passwordHasher.hash("secret-password");

        assertThat(hash).isNotEqualTo("secret-password").startsWith("$2");
        assertThat(passwordEncoder.matches("secret-password", hash)).isTrue();
        assertThat(passwordEncoder.matches("other-password", hash)).isFalse();
    }

    @Test
    void matchesOnlyTheOriginalPassword() {
        String hash = passwordEncoder.encode("secret-password");

        assertThat(passwordHasher.matches("secret-password", hash)).isTrue();
        assertThat(passwordHasher.matches("other-password", hash)).isFalse();
    }
}
