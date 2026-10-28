package com.toneatti.turnos.reservation.application.usecases;

import com.toneatti.turnos.reservation.application.exception.ConcurrentReservationUpdateException;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction;
import com.toneatti.turnos.reservation.domain.model.AppointmentActionOutcome;
import com.toneatti.turnos.reservation.domain.model.ProcessedAppointmentAction;
import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.model.ReservationStatus;
import com.toneatti.turnos.reservation.domain.ports.out.ProcessedAppointmentActionRepository;
import com.toneatti.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.stream.Stream;

import static com.toneatti.turnos.reservation.domain.model.AppointmentActionFixtures.*;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.CANCELLED;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.CONFIRMED;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.CONFIRMING;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.EXPIRED;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.FAILED;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.WAITING_FOR_PHONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplyAppointmentActionUseCaseImplTest {

    private static final Instant NOW = Instant.parse("2099-09-10T14:36:01Z");
    private static final Instant EXPIRES_AT = Instant.parse("2099-09-10T14:44:50Z");

    @Mock
    private ReservationProcessRepository reservationProcessRepository;

    @Mock
    private ProcessedAppointmentActionRepository processedAppointmentActionRepository;

    private ApplyAppointmentActionUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ApplyAppointmentActionUseCaseImpl(reservationProcessRepository,
                processedAppointmentActionRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @ParameterizedTest
    @MethodSource("eventsAndTheStatusTheyLeave")
    void appliesEachEventAndRecordsIt(ReservationStatus from, AppointmentAction action, ReservationStatus to) {
        ReservationProcess process = givenTheProcessIn(from);
        when(reservationProcessRepository.update(7L, process)).thenReturn(Optional.of(process));

        assertThat(useCase.applyAppointmentAction(action)).isEqualTo(AppointmentActionOutcome.APPLIED);

        assertThat(process.getStatus()).isEqualTo(to);
        assertThat(process.getUpdatedAt()).isEqualTo(NOW);
        verify(reservationProcessRepository).update(7L, process);
        assertThat(recorded()).isEqualTo(new ProcessedAppointmentAction(null, action.eventId(),
                action.getClass().getSimpleName(), PROCESS_ID, AppointmentActionOutcome.APPLIED, NOW));
    }

    static Stream<Arguments> eventsAndTheStatusTheyLeave() {
        return Stream.of(
                Arguments.of(CONFIRMING, REQUESTED, WAITING_FOR_PHONE),
                Arguments.of(WAITING_FOR_PHONE, REQUESTED, WAITING_FOR_PHONE),
                Arguments.of(WAITING_FOR_PHONE, CONFIRMATION, CONFIRMED),
                Arguments.of(WAITING_FOR_PHONE, REJECTION, WAITING_FOR_PHONE),
                Arguments.of(WAITING_FOR_PHONE, EXPIRATION, EXPIRED),
                Arguments.of(WAITING_FOR_PHONE, INVALIDATION, FAILED),
                Arguments.of(CONFIRMED, CANCELLATION, CANCELLED));
    }

    // The requestEventId is what the phone step (section 15.5) sends back.
    @Test
    void savesTheEventThatRequestedThePhone() {
        ReservationProcess process = givenTheProcessIn(CONFIRMING);
        when(reservationProcessRepository.update(7L, process)).thenReturn(Optional.of(process));

        useCase.applyAppointmentAction(REQUESTED);

        assertThat(process.getRequestEventId()).isEqualTo("request-event-8a31");
    }

    @Test
    void ignoresADuplicateWithoutTouchingTheProcess() {
        when(processedAppointmentActionRepository.existsByEventId("confirmation-event-7d10")).thenReturn(true);

        assertThat(useCase.applyAppointmentAction(CONFIRMATION)).isEqualTo(AppointmentActionOutcome.DUPLICATE);

        verifyNoInteractions(reservationProcessRepository);
        verify(processedAppointmentActionRepository, never()).create(any());
    }

    // Section 18.3: the event is recorded, so a redelivery is a duplicate, but the process keeps its final status.
    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"CANCELLED", "EXPIRED", "FAILED"})
    void recordsALateEventWithoutReopeningAFinalStatus(ReservationStatus status) {
        ReservationProcess process = givenTheProcessIn(status);

        assertThat(useCase.applyAppointmentAction(CONFIRMATION)).isEqualTo(AppointmentActionOutcome.LATE);

        assertThat(process.getStatus()).isEqualTo(status);
        verify(reservationProcessRepository, never()).update(anyLong(), any());
        assertThat(recorded().outcome()).isEqualTo(AppointmentActionOutcome.LATE);
    }

    @Test
    void recordsALateEventWithoutMovingAConfirmedReservationBack() {
        ReservationProcess process = givenTheProcessIn(CONFIRMED);

        assertThat(useCase.applyAppointmentAction(EXPIRATION)).isEqualTo(AppointmentActionOutcome.LATE);

        assertThat(process.getStatus()).isEqualTo(CONFIRMED);
        verify(reservationProcessRepository, never()).update(anyLong(), any());
    }

    @Test
    void recordsAnEventOfAnUnknownProcess() {
        when(reservationProcessRepository.findByReservationProcessId(PROCESS_ID)).thenReturn(Optional.empty());

        assertThat(useCase.applyAppointmentAction(CONFIRMATION)).isEqualTo(AppointmentActionOutcome.UNKNOWN_PROCESS);

        assertThat(recorded().outcome()).isEqualTo(AppointmentActionOutcome.UNKNOWN_PROCESS);
    }

    // The event is not recorded, so the retry of the consumer processes it again.
    @Test
    void doesNotRecordTheEventWhenTheProcessCannotBeSaved() {
        ReservationProcess process = givenTheProcessIn(WAITING_FOR_PHONE);
        ConcurrentReservationUpdateException conflict = new ConcurrentReservationUpdateException(7L, null);
        when(reservationProcessRepository.update(7L, process)).thenThrow(conflict);

        assertThatThrownBy(() -> useCase.applyAppointmentAction(CONFIRMATION)).isSameAs(conflict);

        verify(processedAppointmentActionRepository, never()).create(any());
    }

    @Test
    void failsWhenTheProcessVanishesBeforeBeingSaved() {
        ReservationProcess process = givenTheProcessIn(WAITING_FOR_PHONE);
        when(reservationProcessRepository.update(7L, process)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.applyAppointmentAction(CONFIRMATION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("vanished");
        verify(processedAppointmentActionRepository, never()).create(any());
    }

    private ReservationProcess givenTheProcessIn(ReservationStatus status) {
        ReservationProcess process = new ReservationProcess(7L, "user-42", 15L, LocalDate.of(2099, 9, 14),
                LocalTime.of(9, 30), status, "hold-6c0c2f41", PROCESS_ID, EXPIRES_AT, null, null, null, null, null,
                NOW.minusSeconds(60), NOW.minusSeconds(60), 3L);
        when(reservationProcessRepository.findByReservationProcessId(PROCESS_ID)).thenReturn(Optional.of(process));
        return process;
    }

    private ProcessedAppointmentAction recorded() {
        ArgumentCaptor<ProcessedAppointmentAction> captor = ArgumentCaptor.forClass(ProcessedAppointmentAction.class);
        verify(processedAppointmentActionRepository).create(captor.capture());
        return captor.getValue();
    }
}
