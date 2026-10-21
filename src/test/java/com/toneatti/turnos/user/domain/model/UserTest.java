package com.toneatti.turnos.user.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void normalizeEmailTrimsAndLowercases() {
        assertThat(User.normalizeEmail("  Juan.Perez@Example.COM ")).isEqualTo("juan.perez@example.com");
    }

    @Test
    void normalizeEmailIgnoresTheDefaultLocale() {
        Locale previous = Locale.getDefault();
        // With a Turkish default locale, "I".toLowerCase() gives a dotless "ı".
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            assertThat(User.normalizeEmail("INFO@EXAMPLE.COM")).isEqualTo("info@example.com");
        } finally {
            Locale.setDefault(previous);
        }
    }
}
