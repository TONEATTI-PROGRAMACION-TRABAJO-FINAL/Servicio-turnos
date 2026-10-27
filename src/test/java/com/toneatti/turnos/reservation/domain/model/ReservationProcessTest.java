package com.toneatti.turnos.reservation.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationProcessTest {

    private static final Instant NOW = Instant.parse("2099-09-10T14:30:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2099-09-10T14:44:50Z");

    @Test
    void startsPendingTheHoldForItsOwner() {
        ReservationProcess process = started();

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.PENDING_HOLD);
        assertThat(process.getUserExternalId()).isEqualTo("user-42");
        assertThat(process.getProfessionalId()).isEqualTo(15L);
        assertThat(process.getDate()).isEqualTo(LocalDate.of(2099, 9, 14));
        assertThat(process.getStartTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(process.getCreatedAt()).isEqualTo(NOW);
        assertThat(process.getUpdatedAt()).isEqualTo(NOW);
        assertThat(process.getId()).isNull();
        assertThat(process.getHoldId()).isNull();
        assertThat(process.getExpiresAt()).isNull();
        assertThat(process.isActive()).isTrue();
    }

    @Test
    void keepsTheIdsOfTheCatedraOnceHeld() {
        ReservationProcess process = held();

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.HELD);
        assertThat(process.getHoldId()).isEqualTo("hold-6c0c2f41");
        assertThat(process.getReservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(process.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(process.getUpdatedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    void waitsForThePhoneWithTheDeadlineOfTheConfirmation() {
        ReservationProcess process = held();
        process.markConfirming(NOW.plusSeconds(2));

        process.markWaitingForPhone(EXPIRES_AT.plusSeconds(60), NOW.plusSeconds(3));

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.WAITING_FOR_PHONE);
        assertThat(process.getExpiresAt()).isEqualTo(EXPIRES_AT.plusSeconds(60));
        assertThat(process.isActive()).isTrue();
    }

    // Reconciled through the list of reservations, which has no deadline.
    @Test
    void keepsTheDeadlineOfTheHoldWhenTheConfirmationHasNone() {
        ReservationProcess process = held();
        process.markConfirming(NOW);

        process.markWaitingForPhone(null, NOW);

        assertThat(process.getExpiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    void keepsTheCodeOfTheCatedraWhenItFails() {
        ReservationProcess process = started();

        process.markFailed("SLOT_ALREADY_HELD", NOW.plusSeconds(1));

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.FAILED);
        assertThat(process.getFailureCode()).isEqualTo("SLOT_ALREADY_HELD");
        assertThat(process.isActive()).isFalse();
    }

    @Test
    void expires() {
        ReservationProcess process = held();

        process.markExpired(EXPIRES_AT);

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(process.isActive()).isFalse();
    }

    @Test
    void rejectsATransitionTheStateMachineDoesNotAllowAndKeepsItsState() {
        ReservationProcess process = started();

        assertThatThrownBy(() -> process.markConfirming(NOW.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING_HOLD to CONFIRMING");
        assertThat(process.getStatus()).isEqualTo(ReservationStatus.PENDING_HOLD);
        assertThat(process.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void aLateAnswerCannotReopenAnExpiredProcess() {
        ReservationProcess process = held();
        process.markExpired(EXPIRES_AT);

        assertThatThrownBy(() -> process.markConfirming(EXPIRES_AT.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(process.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
    }

    @Test
    void isExpiredFromTheDeadlineOfTheCatedraOn() {
        ReservationProcess process = held();

        assertThat(process.isExpiredAt(EXPIRES_AT.minusMillis(1))).isFalse();
        assertThat(process.isExpiredAt(EXPIRES_AT)).isTrue();
    }

    @Test
    void isNotExpiredBeforeHavingADeadline() {
        assertThat(started().isExpiredAt(NOW.plusSeconds(3600))).isFalse();
    }

    private static ReservationProcess started() {
        return ReservationProcess.start("user-42", 15L, LocalDate.of(2099, 9, 14), LocalTime.of(9, 30), NOW);
    }

    private static ReservationProcess held() {
        ReservationProcess process = started();
        process.markHeld("hold-6c0c2f41", "process-a92b47d0", EXPIRES_AT, NOW.plusSeconds(1));
        return process;
    }
}
