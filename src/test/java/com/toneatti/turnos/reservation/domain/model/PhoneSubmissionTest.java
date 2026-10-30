package com.toneatti.turnos.reservation.domain.model;

import org.junit.jupiter.api.Test;

import static com.toneatti.turnos.reservation.domain.model.PhoneSubmissionFixtures.SUBMISSION;
import static org.assertj.core.api.Assertions.assertThat;

class PhoneSubmissionTest {

    @Test
    void doesNotExposeThePhoneNumberInItsText() {
        assertThat(SUBMISSION.toString()).doesNotContain("5551234").contains("process-a92b47d0");
    }
}
