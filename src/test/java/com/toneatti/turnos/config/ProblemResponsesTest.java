package com.toneatti.turnos.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemResponsesTest {

    @Test
    void carriesTheStatusTheDetailAndTheCode() {
        ResponseStatusException exception =
                ProblemResponses.problem(HttpStatus.NOT_FOUND, "PROFESSIONAL_NOT_FOUND", "Professional not found");

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getBody().getStatus()).isEqualTo(404);
        assertThat(exception.getBody().getDetail()).isEqualTo("Professional not found");
        assertThat(exception.getBody().getProperties()).containsEntry("code", "PROFESSIONAL_NOT_FOUND");
    }
}
