package com.toneatti.turnos.reservation.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationStatusTest {

    @ParameterizedTest
    @CsvSource({
            "PENDING_HOLD, HELD",
            "PENDING_HOLD, FAILED",
            "HELD, CONFIRMING",
            "HELD, EXPIRED",
            "HELD, FAILED",
            "CONFIRMING, WAITING_FOR_PHONE",
            "CONFIRMING, EXPIRED",
            "CONFIRMING, FAILED"
    })
    void allowsTheTransitionsOfTheStateMachine(ReservationStatus from, ReservationStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @Test
    void allowsNoOtherTransition() {
        long allowed = Arrays.stream(ReservationStatus.values())
                .mapToLong(from -> Arrays.stream(ReservationStatus.values()).filter(from::canTransitionTo).count())
                .sum();

        assertThat(allowed).isEqualTo(8);
    }

    @Test
    void aPendingHoldCannotExpireBecauseItHasNoDeadlineYet() {
        assertThat(ReservationStatus.PENDING_HOLD.canTransitionTo(ReservationStatus.EXPIRED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"EXPIRED", "FAILED"})
    void finalStatusesCannotBeReopened(ReservationStatus status) {
        assertThat(status.isFinal()).isTrue();
        assertThat(ReservationStatus.values()).noneMatch(status::canTransitionTo);
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"EXPIRED", "FAILED"}, mode = EnumSource.Mode.EXCLUDE)
    void theOtherStatusesAreNotFinal(ReservationStatus status) {
        assertThat(status.isFinal()).isFalse();
    }
}
