package com.toneatti.turnos.reservation.domain.ports.out;

import com.toneatti.turnos.reservation.domain.model.PhoneSubmission;

/**
 * Sends the phone number to the cátedra. Returns once the cátedra received it; the outcome arrives later as an event
 * of the process. When it cannot confirm the delivery it throws, and the event may have been delivered or not.
 */
public interface PhoneSubmissionPublisher {

    void publish(PhoneSubmission submission);

}
