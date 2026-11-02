package com.toneatti.turnos.reservation.application.usecases;

import com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException;
import com.toneatti.turnos.catedra.domain.exception.CatedraUnavailableException;
import com.toneatti.turnos.catedra.domain.model.Appointment;
import com.toneatti.turnos.catedra.domain.model.AppointmentCancellation;
import com.toneatti.turnos.catedra.domain.model.AppointmentStatus;
import com.toneatti.turnos.catedra.domain.ports.out.CatedraRepository;
import com.toneatti.turnos.reservation.application.exception.CancellationPendingException;
import com.toneatti.turnos.reservation.application.exception.ConcurrentReservationUpdateException;
import com.toneatti.turnos.reservation.application.exception.ReservationNotCancellableException;
import com.toneatti.turnos.reservation.application.exception.ReservationUnavailableException;
import com.toneatti.turnos.reservation.application.exception.UnexpectedCatedraRejectionException;
import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.model.ReservationStatus;
import com.toneatti.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelReservationUseCaseImplTest {

    private static final Instant NOW = Instant.parse("2099-09-11T15:10:00Z");
    private static final String USER_ID = "user-42";
    private static final String PROCESS_ID = "process-a92b47d0";
    private static final String REASON = "Cambio solicitado por el usuario";
    private static final LocalDate DAY = LocalDate.of(2099, 9, 14);
    private static final AppointmentCancellation CANCELLATION = new AppointmentCancellation(8251L, PROCESS_ID, NOW);

    @Mock
    private ReservationProcessRepository reservationProcessRepository;

    @Mock
    private CatedraRepository catedraRepository;

    private CancelReservationUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new CancelReservationUseCaseImpl(reservationProcessRepository, catedraRepository,
                Clock.fixed(NOW, ZoneId.of("America/Argentina/Buenos_Aires")));
    }

    @Test
    void cancelsAConfirmedReservationOfTheUserInTheCatedra() {
        givenTheStoredProcess(processIn(ReservationStatus.CONFIRMED));
        when(catedraRepository.cancelAppointment(PROCESS_ID, REASON)).thenReturn(CANCELLATION);
        givenUpdatesSucceed();

        assertThat(useCase.cancelReservation(USER_ID, PROCESS_ID, REASON)).hasValueSatisfying(this::assertCancelled);
    }

    @Test
    void sendsNoReasonWhenItIsBlank() {
        givenTheStoredProcess(processIn(ReservationStatus.CONFIRMED));
        when(catedraRepository.cancelAppointment(PROCESS_ID, null)).thenReturn(CANCELLATION);
        givenUpdatesSucceed();

        assertThat(useCase.cancelReservation(USER_ID, PROCESS_ID, "  ")).hasValueSatisfying(cancelled ->
                assertThat(cancelled.getCancellationReason()).isNull());
    }

    // The owner is checked before calling the cátedra, which lists the reservations of every user.
    @Test
    void returnsEmptyWithoutCallingTheCatedraWhenTheReservationIsMissingOrBelongsToAnotherUser() {
        when(reservationProcessRepository.findByReservationProcessIdAndUserExternalId(PROCESS_ID, "user-43"))
                .thenReturn(Optional.empty());

        assertThat(useCase.cancelReservation("user-43", PROCESS_ID, REASON)).isEmpty();
        verifyNoInteractions(catedraRepository);
    }

    // Cancelled before, by this endpoint or by the AppointmentCancelled event.
    @Test
    void returnsAnAlreadyCancelledReservationWithoutCallingTheCatedra() {
        ReservationProcess cancelled = processIn(ReservationStatus.CONFIRMED);
        cancelled.markCancelled(REASON, NOW);
        givenTheStoredProcess(cancelled);

        assertThat(useCase.cancelReservation(USER_ID, PROCESS_ID, "Otro motivo")).hasValue(cancelled);
        verifyNoInteractions(catedraRepository);
        verify(reservationProcessRepository, never()).update(anyLong(), any());
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"CONFIRMED", "CANCELLED"},
            mode = EnumSource.Mode.EXCLUDE)
    void rejectsWithoutCallingTheCatedraAReservationThatIsNotConfirmed(ReservationStatus status) {
        givenTheStoredProcess(processIn(status));

        assertThatThrownBy(() -> useCase.cancelReservation(USER_ID, PROCESS_ID, REASON))
                .isInstanceOf(ReservationNotCancellableException.class);
        verifyNoInteractions(catedraRepository);
        verify(reservationProcessRepository, never()).update(anyLong(), any());
    }

    @Test
    void keepsTheReservationConfirmedWhenTheCatedraDoesNotAllowCancellingIt() {
        givenTheStoredProcess(processIn(ReservationStatus.CONFIRMED));
        when(catedraRepository.cancelAppointment(PROCESS_ID, REASON))
                .thenThrow(rejected(409, CatedraRequestRejectedException.RESERVATION_NOT_CANCELLABLE));

        assertThatThrownBy(() -> useCase.cancelReservation(USER_ID, PROCESS_ID, REASON))
                .isInstanceOf(ReservationNotCancellableException.class);
        verify(reservationProcessRepository, never()).update(anyLong(), any());
    }

    @Test
    void failsAsUnavailableWhenTheCatedraDoesNotFindTheIntegration() {
        givenTheStoredProcess(processIn(ReservationStatus.CONFIRMED));
        when(catedraRepository.cancelAppointment(PROCESS_ID, REASON))
                .thenThrow(rejected(403, CatedraRequestRejectedException.STUDENT_INTEGRATION_NOT_FOUND));

        assertThatThrownBy(() -> useCase.cancelReservation(USER_ID, PROCESS_ID, REASON))
                .isInstanceOf(ReservationUnavailableException.class);
    }

    // The owner was already checked locally, so these mean a bug of this service.
    @ParameterizedTest
    @CsvSource({"404, RESERVATION_NOT_FOUND", "403, RESERVATION_OWNER_MISMATCH", "400, VALIDATION_ERROR"})
    void failsAsUnexpectedWithAnyOtherRejection(int status, String code) {
        givenTheStoredProcess(processIn(ReservationStatus.CONFIRMED));
        when(catedraRepository.cancelAppointment(PROCESS_ID, REASON)).thenThrow(rejected(status, code));

        assertThatThrownBy(() -> useCase.cancelReservation(USER_ID, PROCESS_ID, REASON))
                .isInstanceOf(UnexpectedCatedraRejectionException.class)
                .hasMessageContaining(code);
        verify(reservationProcessRepository, never()).update(anyLong(), any());
    }

    @Test
    void doesNotCancelAgainWhenTheCancellationWithoutAnswerReachedTheCatedra() {
        givenTheStoredProcess(processIn(ReservationStatus.CONFIRMED));
        when(catedraRepository.cancelAppointment(PROCESS_ID, REASON)).thenThrow(timeout());
        givenTheCatedraHas(AppointmentStatus.CANCELLED);
        givenUpdatesSucceed();

        assertThat(useCase.cancelReservation(USER_ID, PROCESS_ID, REASON)).hasValueSatisfying(this::assertCancelled);
        verify(catedraRepository, times(1)).cancelAppointment(PROCESS_ID, REASON);
    }

    @Test
    void cancelsAgainWhenTheCancellationWithoutAnswerDidNotReachTheCatedra() {
        givenTheStoredProcess(processIn(ReservationStatus.CONFIRMED));
        when(catedraRepository.cancelAppointment(PROCESS_ID, REASON)).thenThrow(timeout()).thenReturn(CANCELLATION);
        givenTheCatedraHas(AppointmentStatus.CONFIRMED);
        givenUpdatesSucceed();

        assertThat(useCase.cancelReservation(USER_ID, PROCESS_ID, REASON)).hasValueSatisfying(this::assertCancelled);
        verify(catedraRepository, times(2)).cancelAppointment(PROCESS_ID, REASON);
    }

    @Test
    void staysPendingAfterTheLastAttemptWithoutAnswer() {
        givenTheStoredProcess(processIn(ReservationStatus.CONFIRMED));
        when(catedraRepository.cancelAppointment(PROCESS_ID, REASON)).thenThrow(timeout());
        givenTheCatedraHas(AppointmentStatus.CONFIRMED);

        assertStaysPendingAfterCalling(CancelReservationUseCaseImpl.MAX_ATTEMPTS);
    }

    @Test
    void staysPendingWithoutCancellingAgainWhenTheReconciliationGetsNoAnswer() {
        givenTheStoredProcess(processIn(ReservationStatus.CONFIRMED));
        when(catedraRepository.cancelAppointment(PROCESS_ID, REASON)).thenThrow(timeout());
        when(catedraRepository.findAppointments(USER_ID, DAY, DAY)).thenThrow(timeout());

        assertStaysPendingAfterCalling(1);
    }

    @Test
    void staysPendingWithoutCancellingAgainWhenTheCatedraDoesNotListTheReservation() {
        givenTheStoredProcess(processIn(ReservationStatus.CONFIRMED));
        when(catedraRepository.cancelAppointment(PROCESS_ID, REASON)).thenThrow(timeout());
        when(catedraRepository.findAppointments(USER_ID, DAY, DAY)).thenReturn(List.of());

        assertStaysPendingAfterCalling(1);
    }

    // The AppointmentCancelled event was applied between reading the process and saving it.
    @Test
    void returnsTheProcessTheCancellationEventLeftWhenItWasSavedFirst() {
        givenTheStoredProcess(processIn(ReservationStatus.CONFIRMED));
        when(catedraRepository.cancelAppointment(PROCESS_ID, REASON)).thenReturn(CANCELLATION);
        when(reservationProcessRepository.update(eq(7L), any()))
                .thenThrow(new ConcurrentReservationUpdateException(7L, null));
        ReservationProcess cancelledByTheEvent = processIn(ReservationStatus.CONFIRMED);
        cancelledByTheEvent.markCancelled(REASON, NOW);
        when(reservationProcessRepository.findByReservationProcessIdAndUserExternalId(PROCESS_ID, USER_ID))
                .thenReturn(Optional.of(processIn(ReservationStatus.CONFIRMED)), Optional.of(cancelledByTheEvent));

        assertThat(useCase.cancelReservation(USER_ID, PROCESS_ID, REASON)).hasValue(cancelledByTheEvent);
    }

    private void assertStaysPendingAfterCalling(int cancellations) {
        assertThatThrownBy(() -> useCase.cancelReservation(USER_ID, PROCESS_ID, REASON))
                .isInstanceOf(CancellationPendingException.class);
        verify(catedraRepository, times(cancellations)).cancelAppointment(PROCESS_ID, REASON);
        verify(reservationProcessRepository, never()).update(anyLong(), any());
    }

    private void assertCancelled(ReservationProcess process) {
        assertThat(process.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(process.getCancellationReason()).isEqualTo(REASON);
        assertThat(process.getUpdatedAt()).isEqualTo(NOW);
    }

    private void givenTheStoredProcess(ReservationProcess process) {
        when(reservationProcessRepository.findByReservationProcessIdAndUserExternalId(PROCESS_ID, USER_ID))
                .thenReturn(Optional.of(process));
    }

    private void givenUpdatesSucceed() {
        when(reservationProcessRepository.update(eq(7L), any()))
                .thenAnswer(invocation -> Optional.of(invocation.getArgument(1)));
    }

    private void givenTheCatedraHas(AppointmentStatus status) {
        when(catedraRepository.findAppointments(USER_ID, DAY, DAY)).thenReturn(List.of(
                new Appointment(8251L, "process-other", USER_ID, 15L, DAY, LocalTime.of(10, 0),
                        AppointmentStatus.CONFIRMED),
                new Appointment(8250L, PROCESS_ID, USER_ID, 15L, DAY, LocalTime.of(9, 30), status)));
    }

    private static CatedraRequestRejectedException rejected(int status, String code) {
        return new CatedraRequestRejectedException(status, code, "Catedra rejected with " + code, null);
    }

    private static CatedraUnavailableException timeout() {
        return new CatedraUnavailableException("Read timed out", null);
    }

    private static ReservationProcess processIn(ReservationStatus status) {
        return new ReservationProcess(7L, USER_ID, 15L, DAY, LocalTime.of(9, 30), status, "hold-6c0c2f41",
                PROCESS_ID, NOW.minusSeconds(600), null, "request-event-8a31", 8250L, null, null, NOW, NOW, 3L);
    }
}
