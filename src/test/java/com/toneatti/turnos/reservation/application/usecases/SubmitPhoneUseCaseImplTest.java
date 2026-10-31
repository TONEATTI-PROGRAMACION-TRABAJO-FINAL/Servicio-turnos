package com.toneatti.turnos.reservation.application.usecases;

import com.toneatti.turnos.reservation.application.exception.ConcurrentReservationUpdateException;
import com.toneatti.turnos.reservation.application.exception.InvalidPhoneNumberException;
import com.toneatti.turnos.reservation.application.exception.PhoneNotRequestedException;
import com.toneatti.turnos.reservation.application.exception.PhoneSubmissionUnavailableException;
import com.toneatti.turnos.reservation.application.exception.ReservationExpiredException;
import com.toneatti.turnos.reservation.application.exception.ReservationNotWaitingForPhoneException;
import com.toneatti.turnos.reservation.domain.model.PhoneNumber;
import com.toneatti.turnos.reservation.domain.model.PhoneSubmission;
import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.model.ReservationStatus;
import com.toneatti.turnos.reservation.domain.ports.out.PhoneSubmissionPublisher;
import com.toneatti.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.toneatti.turnos.reservation.domain.model.AppointmentActionFixtures.REJECTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitPhoneUseCaseImplTest {

    private static final Instant NOW = Instant.parse("2099-09-10T14:36:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2099-09-10T14:44:50Z");
    private static final String USER_ID = "user-42";
    private static final String TYPED_PHONE = "+54 9 (261) 555-1234";

    @Mock
    private ReservationProcessRepository reservationProcessRepository;

    @Mock
    private PhoneSubmissionPublisher phoneSubmissionPublisher;

    private SubmitPhoneUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SubmitPhoneUseCaseImpl(reservationProcessRepository, phoneSubmissionPublisher,
                Clock.fixed(NOW, ZoneId.of("America/Argentina/Buenos_Aires")));
    }

    @Test
    void publishesTheNormalizedPhoneForTheEventThatRequestedIt() {
        givenTheStoredProcess(waitingForPhone());
        givenUpdatesSucceed();

        assertThat(useCase.submitPhone(USER_ID, 7L, TYPED_PHONE)).isPresent();

        PhoneSubmission submission = publishedSubmissions(1).getFirst();
        assertThat(submission).usingRecursiveComparison().ignoringFields("eventId").isEqualTo(new PhoneSubmission(
                null, "process-a92b47d0", "hold-6c0c2f41", "request-event-8a31",
                new PhoneNumber("+5492615551234"), NOW));
        assertThat(UUID.fromString(submission.eventId())).isNotNull();
    }

    // Section 15.7: a phone sent again after a rejection is another event.
    @Test
    void publishesEachSubmissionWithANewEventId() {
        givenTheStoredProcess(waitingForPhone());
        givenUpdatesSucceed();

        useCase.submitPhone(USER_ID, 7L, TYPED_PHONE);
        useCase.submitPhone(USER_ID, 7L, TYPED_PHONE);

        List<PhoneSubmission> submissions = publishedSubmissions(2);
        assertThat(submissions.get(0).eventId()).isNotEqualTo(submissions.get(1).eventId());
    }

    // So the app does not take the rejection of the previous phone as the answer to this one.
    @Test
    void savesTheProcessWithoutThePreviousRejectionBeforePublishing() {
        ReservationProcess process = waitingForPhone();
        process.apply(REJECTION, NOW);
        givenTheStoredProcess(process);
        givenUpdatesSucceed();

        Optional<ReservationProcess> submitted = useCase.submitPhone(USER_ID, 7L, TYPED_PHONE);

        assertThat(submitted).hasValueSatisfying(saved -> {
            assertThat(saved.getStatus()).isEqualTo(ReservationStatus.WAITING_FOR_PHONE);
            assertThat(saved.getPhoneRejectionReason()).isNull();
        });
        InOrder order = inOrder(reservationProcessRepository, phoneSubmissionPublisher);
        order.verify(reservationProcessRepository).update(eq(7L), any());
        order.verify(phoneSubmissionPublisher).publish(any());
    }

    @Test
    void rejectsAnInvalidPhoneBeforeReadingTheProcess() {
        assertThatThrownBy(() -> useCase.submitPhone(USER_ID, 7L, "261-555"))
                .isInstanceOf(InvalidPhoneNumberException.class)
                .hasMessageNotContaining("261");
        verifyNoInteractions(reservationProcessRepository, phoneSubmissionPublisher);
    }

    @Test
    void returnsEmptyWhenTheProcessDoesNotExistOrBelongsToAnotherUser() {
        when(reservationProcessRepository.findByIdAndUserExternalId(7L, USER_ID)).thenReturn(Optional.empty());

        assertThat(useCase.submitPhone(USER_ID, 7L, TYPED_PHONE)).isEmpty();
        verifyNoInteractions(phoneSubmissionPublisher);
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = "WAITING_FOR_PHONE", mode = EnumSource.Mode.EXCLUDE)
    void rejectsAProcessThatIsNotWaitingForThePhone(ReservationStatus status) {
        givenTheStoredProcess(processIn(status, "request-event-8a31", EXPIRES_AT));

        assertThatThrownBy(() -> useCase.submitPhone(USER_ID, 7L, TYPED_PHONE))
                .isInstanceOf(ReservationNotWaitingForPhoneException.class);
        assertNothingWasSavedNorPublished();
    }

    // The answer of the confirmation was saved before the event that requests the phone arrived.
    @Test
    void rejectsAProcessWhosePhoneWasNotRequestedYet() {
        givenTheStoredProcess(processIn(ReservationStatus.WAITING_FOR_PHONE, null, EXPIRES_AT));

        assertThatThrownBy(() -> useCase.submitPhone(USER_ID, 7L, TYPED_PHONE))
                .isInstanceOf(PhoneNotRequestedException.class);
        assertNothingWasSavedNorPublished();
    }

    @Test
    void rejectsAProcessFromItsDeadlineOn() {
        givenTheStoredProcess(processIn(ReservationStatus.WAITING_FOR_PHONE, "request-event-8a31", NOW));

        assertThatThrownBy(() -> useCase.submitPhone(USER_ID, 7L, TYPED_PHONE))
                .isInstanceOf(ReservationExpiredException.class);
        assertNothingWasSavedNorPublished();
    }

    @Test
    void acceptsAPhoneJustBeforeTheDeadline() {
        givenTheStoredProcess(processIn(ReservationStatus.WAITING_FOR_PHONE, "request-event-8a31",
                NOW.plusMillis(1)));
        givenUpdatesSucceed();

        assertThat(useCase.submitPhone(USER_ID, 7L, TYPED_PHONE)).isPresent();
        verify(phoneSubmissionPublisher).publish(any());
    }

    // An event of the cátedra changed the process after it was read, for example it was confirmed.
    @Test
    void publishesNothingWhenTheProcessChangedMeanwhile() {
        givenTheStoredProcess(waitingForPhone());
        when(reservationProcessRepository.update(eq(7L), any()))
                .thenThrow(new ConcurrentReservationUpdateException(7L, null));

        assertThatThrownBy(() -> useCase.submitPhone(USER_ID, 7L, TYPED_PHONE))
                .isInstanceOf(ConcurrentReservationUpdateException.class);
        verifyNoInteractions(phoneSubmissionPublisher);
    }

    @Test
    void returnsEmptyWhenTheProcessWasDeletedMeanwhile() {
        givenTheStoredProcess(waitingForPhone());
        when(reservationProcessRepository.update(eq(7L), any())).thenReturn(Optional.empty());

        assertThat(useCase.submitPhone(USER_ID, 7L, TYPED_PHONE)).isEmpty();
        verifyNoInteractions(phoneSubmissionPublisher);
    }

    @Test
    void failsWhenThePhoneCannotBePublished() {
        givenTheStoredProcess(waitingForPhone());
        givenUpdatesSucceed();
        doThrow(new PhoneSubmissionUnavailableException("Kafka is down", null))
                .when(phoneSubmissionPublisher).publish(any());

        assertThatThrownBy(() -> useCase.submitPhone(USER_ID, 7L, TYPED_PHONE))
                .isInstanceOf(PhoneSubmissionUnavailableException.class);
    }

    private void givenTheStoredProcess(ReservationProcess process) {
        when(reservationProcessRepository.findByIdAndUserExternalId(7L, USER_ID)).thenReturn(Optional.of(process));
    }

    private void givenUpdatesSucceed() {
        when(reservationProcessRepository.update(eq(7L), any()))
                .thenAnswer(invocation -> Optional.of(invocation.getArgument(1)));
    }

    private List<PhoneSubmission> publishedSubmissions(int count) {
        ArgumentCaptor<PhoneSubmission> submissions = ArgumentCaptor.forClass(PhoneSubmission.class);
        verify(phoneSubmissionPublisher, times(count)).publish(submissions.capture());
        return submissions.getAllValues();
    }

    private void assertNothingWasSavedNorPublished() {
        verify(reservationProcessRepository, never()).update(anyLong(), any());
        verifyNoInteractions(phoneSubmissionPublisher);
    }

    private static ReservationProcess waitingForPhone() {
        return processIn(ReservationStatus.WAITING_FOR_PHONE, "request-event-8a31", EXPIRES_AT);
    }

    private static ReservationProcess processIn(ReservationStatus status, String requestEventId, Instant expiresAt) {
        return new ReservationProcess(7L, USER_ID, 15L, LocalDate.of(2099, 9, 14), LocalTime.of(9, 30), status,
                "hold-6c0c2f41", "process-a92b47d0", expiresAt, null, requestEventId, null, null, null, NOW, NOW, 0L);
    }
}
