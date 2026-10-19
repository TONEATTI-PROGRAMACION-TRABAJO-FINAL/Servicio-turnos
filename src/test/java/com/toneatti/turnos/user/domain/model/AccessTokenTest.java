package com.toneatti.turnos.user.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTokenTest {

    @Test
    void toStringDoesNotExposeTheToken() {
        AccessToken token = new AccessToken("signed-token", Instant.EPOCH, Instant.EPOCH.plusSeconds(60));

        assertThat(token.toString()).doesNotContain("signed-token").contains("1970-01-01T00:01:00Z");
    }
}
