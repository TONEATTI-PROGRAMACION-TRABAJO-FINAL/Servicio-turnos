package com.toneatti.turnos.user.infrastructure.security;

import com.toneatti.turnos.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class Argon2PasswordHasherAdapterTest {

    // The same encoder the application uses, so the test also checks its Argon2id parameters.
    private final PasswordEncoder passwordEncoder = new SecurityConfig().passwordEncoder();
    private final Argon2PasswordHasherAdapter passwordHasher = new Argon2PasswordHasherAdapter(passwordEncoder);

    @Test
    void hashIsNotThePasswordAndMatchesIt() {
        String hash = passwordHasher.hash("secret-password");

        assertThat(hash).isNotEqualTo("secret-password").startsWith("$argon2id$v=19$m=19456,t=2,p=1$");
        assertThat(passwordEncoder.matches("secret-password", hash)).isTrue();
        assertThat(passwordEncoder.matches("other-password", hash)).isFalse();
    }

    @Test
    void matchesOnlyTheOriginalPassword() {
        String hash = passwordEncoder.encode("secret-password");

        assertThat(passwordHasher.matches("secret-password", hash)).isTrue();
        assertThat(passwordHasher.matches("other-password", hash)).isFalse();
    }

    @Test
    void usesARandomSaltPerHash() {
        // Two users with the same password get different hashes.
        assertThat(passwordHasher.hash("secret-password")).isNotEqualTo(passwordHasher.hash("secret-password"));
    }
}
