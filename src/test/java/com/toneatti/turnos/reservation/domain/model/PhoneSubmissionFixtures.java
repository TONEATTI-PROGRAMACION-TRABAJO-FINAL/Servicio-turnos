package com.toneatti.turnos.reservation.domain.model;

import java.time.Instant;

/**
 * The example of section 15.5 of the integration contract, moved to 2099.
 */
public final class PhoneSubmissionFixtures {

    public static final String PHONE_NUMBER = "+5492615551234";
    public static final PhoneSubmission SUBMISSION = submission("process-a92b47d0");

    private PhoneSubmissionFixtures() {
    }

    public static PhoneSubmission submission(String reservationProcessId) {
        return new PhoneSubmission("phone-submitted-b91e", reservationProcessId, "hold-6c0c2f41",
                "request-event-8a31", new PhoneNumber(PHONE_NUMBER), Instant.parse("2099-09-10T14:36:00Z"));
    }
}
