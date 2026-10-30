package com.toneatti.turnos.reservation.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.stream.Stream;

import static com.toneatti.turnos.reservation.domain.model.AppointmentActionFixtures.*;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.CANCELLED;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.CONFIRMED;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.CONFIRMING;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.EXPIRED;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.FAILED;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.HELD;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.WAITING_FOR_PHONE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationProcessTest {

    private static final Instant NOW = Instant.parse("2099-09-10T14:30:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2099-09-10T14:44:50Z");
    private static final Instant LATER = NOW.plusSeconds(30);

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
        assertThat(process.getExpiresAt()).isEqualTo(REQUESTED.expiresAt());
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

    @Test
    void keepsTheEventThatRequestedThePhoneAndItsDeadline() {
        ReservationProcess process = processIn(WAITING_FOR_PHONE);

        assertThat(process.apply(REQUESTED, LATER)).isTrue();

        assertThat(process.getStatus()).isEqualTo(WAITING_FOR_PHONE);
        assertThat(process.getRequestEventId()).isEqualTo("request-event-8a31");
        assertThat(process.getExpiresAt()).isEqualTo(REQUESTED.expiresAt());
        assertThat(process.getUpdatedAt()).isEqualTo(LATER);
    }

    // The event can arrive before the answer of the confirmation is saved.
    @Test
    void waitsForThePhoneWhenItIsRequestedBeforeTheConfirmationIsSaved() {
        ReservationProcess process = processIn(CONFIRMING);

        assertThat(process.apply(REQUESTED, LATER)).isTrue();

        assertThat(process.getStatus()).isEqualTo(WAITING_FOR_PHONE);
        assertThat(process.getRequestEventId()).isEqualTo("request-event-8a31");
    }

    @Test
    void keepsTheReservationOfTheCatedraOnceConfirmed() {
        ReservationProcess process = processIn(WAITING_FOR_PHONE);

        assertThat(process.apply(CONFIRMATION, LATER)).isTrue();

        assertThat(process.getStatus()).isEqualTo(CONFIRMED);
        assertThat(process.getCatedraReservationId()).isEqualTo(8251L);
        assertThat(process.getUpdatedAt()).isEqualTo(LATER);
        assertThat(process.isActive()).isTrue();
    }

    @Test
    void keepsWaitingForAValidPhoneWhenOneIsRejected() {
        ReservationProcess process = processIn(WAITING_FOR_PHONE);

        assertThat(process.apply(REJECTION, LATER)).isTrue();

        assertThat(process.getStatus()).isEqualTo(WAITING_FOR_PHONE);
        assertThat(process.getPhoneRejectionReason()).isEqualTo("INVALID_PHONE_NUMBER");
        assertThat(process.getExpiresAt()).isEqualTo(REJECTION.expiresAt());
        assertThat(process.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    void forgetsTheRejectionOfThePreviousPhoneWhenAnotherIsSubmitted() {
        ReservationProcess process = processIn(WAITING_FOR_PHONE);
        process.apply(REJECTION, NOW);

        process.submitPhone(LATER);

        assertThat(process.getStatus()).isEqualTo(WAITING_FOR_PHONE);
        assertThat(process.getPhoneRejectionReason()).isNull();
        assertThat(process.getExpiresAt()).isEqualTo(REJECTION.expiresAt());
        assertThat(process.getUpdatedAt()).isEqualTo(LATER);
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = "WAITING_FOR_PHONE", mode = EnumSource.Mode.EXCLUDE)
    void acceptsAPhoneOnlyWhileWaitingForIt(ReservationStatus status) {
        ReservationProcess process = processIn(status);

        assertThatThrownBy(() -> process.submitPhone(LATER)).isInstanceOf(IllegalStateException.class);
        assertThat(process).usingRecursiveComparison().isEqualTo(processIn(status));
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"HELD", "CONFIRMING", "WAITING_FOR_PHONE"})
    void expiresWithTheReasonOfTheCatedra(ReservationStatus status) {
        ReservationProcess process = processIn(status);

        assertThat(process.apply(EXPIRATION, LATER)).isTrue();

        assertThat(process.getStatus()).isEqualTo(EXPIRED);
        assertThat(process.getFailureCode()).isEqualTo("HOLD_EXPIRED");
        assertThat(process.isActive()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"HELD", "CONFIRMING", "WAITING_FOR_PHONE"})
    void failsWithTheReasonOfTheCatedraWhenTheProcessIsInvalid(ReservationStatus status) {
        ReservationProcess process = processIn(status);

        assertThat(process.apply(INVALIDATION, LATER)).isTrue();

        assertThat(process.getStatus()).isEqualTo(FAILED);
        assertThat(process.getFailureCode()).isEqualTo("REQUEST_EVENT_MISMATCH");
        assertThat(process.isActive()).isFalse();
    }

    @Test
    void cancelsAConfirmedReservationWithItsReason() {
        ReservationProcess process = processIn(CONFIRMED);

        assertThat(process.apply(CANCELLATION, LATER)).isTrue();

        assertThat(process.getStatus()).isEqualTo(CANCELLED);
        assertThat(process.getCancellationReason()).isEqualTo("Cambio solicitado por el usuario");
        assertThat(process.isActive()).isFalse();
    }

    // Section 18.3: a late event cannot reopen a final status nor move a confirmed reservation back, and an event
    // that the status does not expect yet changes nothing either.
    @ParameterizedTest
    @MethodSource("eventsTheStatusDoesNotAccept")
    void ignoresAnEventItsStatusDoesNotAccept(ReservationStatus status, AppointmentAction action) {
        ReservationProcess process = processIn(status);

        assertThat(process.apply(action, LATER)).isFalse();

        assertThat(process).usingRecursiveComparison().isEqualTo(processIn(status));
    }

    static Stream<Arguments> eventsTheStatusDoesNotAccept() {
        Stream<Arguments> late = Stream.of(CANCELLED, EXPIRED, FAILED)
                .flatMap(status -> ALL.stream().map(action -> Arguments.of(status, action)));
        Stream<Arguments> afterConfirmed = ALL.stream()
                .filter(action -> action != CANCELLATION)
                .map(action -> Arguments.of(CONFIRMED, action));
        Stream<Arguments> early = Stream.of(
                Arguments.of(HELD, REQUESTED),
                Arguments.of(CONFIRMING, CONFIRMATION),
                Arguments.of(CONFIRMING, REJECTION),
                Arguments.of(WAITING_FOR_PHONE, CANCELLATION));
        return Stream.of(late, afterConfirmed, early).flatMap(arguments -> arguments);
    }

    private static ReservationProcess processIn(ReservationStatus status) {
        return new ReservationProcess(7L, "user-42", 15L, LocalDate.of(2099, 9, 14), LocalTime.of(9, 30), status,
                "hold-6c0c2f41", PROCESS_ID, EXPIRES_AT, null, null, null, null, null, NOW, NOW, 3L);
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
