package com.toneatti.turnos.reservation.application.usecases;

import com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException;
import com.toneatti.turnos.catedra.domain.exception.CatedraUnavailableException;
import com.toneatti.turnos.catedra.domain.model.Appointment;
import com.toneatti.turnos.catedra.domain.model.AppointmentStatus;
import com.toneatti.turnos.catedra.domain.model.Hold;
import com.toneatti.turnos.catedra.domain.model.HoldConfirmation;
import com.toneatti.turnos.catedra.domain.model.Occupancy;
import com.toneatti.turnos.catedra.domain.model.OccupancyStatus;
import com.toneatti.turnos.catedra.domain.ports.out.CatedraRepository;
import com.toneatti.turnos.reservation.application.exception.HoldExpiredException;
import com.toneatti.turnos.reservation.application.exception.ReservationPendingException;
import com.toneatti.turnos.reservation.application.exception.ReservationRejectedException;
import com.toneatti.turnos.reservation.application.exception.ReservationUnavailableException;
import com.toneatti.turnos.reservation.application.exception.SlotInThePastException;
import com.toneatti.turnos.reservation.application.exception.UnexpectedCatedraRejectionException;
import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.model.ReservationStatus;
import com.toneatti.turnos.reservation.domain.model.StartedReservation;
import com.toneatti.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.domain.ports.out.UserRepository;
import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.CONFIRMING;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.EXPIRED;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.FAILED;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.HELD;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.PENDING_HOLD;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.WAITING_FOR_PHONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartReservationUseCaseImplTest {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Instant NOW = Instant.parse("2099-09-10T14:30:00Z");
    private static final Instant EXPIRES_AT = NOW.plusSeconds(600);
    private static final LocalDate DAY = LocalDate.of(2099, 9, 14);
    private static final LocalTime NINE_THIRTY = LocalTime.of(9, 30);
    private static final String USER_ID = "3f1c2b8e-0000-4000-8000-000000000001";
    private static final User USER = new User(1L, USER_ID, "juan@example.com", "hash", "Juan", "Perez", NOW);
    private static final String HOLD_ID = "hold-6c0c2f41";
    private static final Hold HOLD = new Hold(HOLD_ID, "process-a92b47d0", EXPIRES_AT);

    @Mock
    private CatedraRepository catedraRepository;

    @Mock
    private UserRepository userRepository;

    private final InMemoryReservationProcessRepository processes = new InMemoryReservationProcessRepository();
    private StartReservationUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new StartReservationUseCaseImpl(processes, catedraRepository, userRepository,
                Clock.fixed(NOW, ZONE));
    }

    @Test
    void holdsAndConfirmsTheSlotForTheAuthenticatedUser() {
        givenTheUser();
        when(catedraRepository.createHold(15L, DAY, NINE_THIRTY, USER_ID)).thenReturn(HOLD);
        when(catedraRepository.confirmHold(HOLD_ID, USER_ID, "Juan", "Perez"))
                .thenReturn(new HoldConfirmation("process-a92b47d0", EXPIRES_AT.plusSeconds(60)));

        StartedReservation started = start();

        assertThat(started.resumed()).isFalse();
        ReservationProcess process = started.process();
        assertThat(process.getStatus()).isEqualTo(WAITING_FOR_PHONE);
        assertThat(process.getUserExternalId()).isEqualTo(USER_ID);
        assertThat(process.getHoldId()).isEqualTo(HOLD_ID);
        assertThat(process.getReservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(process.getExpiresAt()).isEqualTo(EXPIRES_AT.plusSeconds(60));
        // Each status is saved before the call to the cátedra that leaves it.
        assertThat(processes.savedStatuses).containsExactly(PENDING_HOLD, HELD, CONFIRMING, WAITING_FOR_PHONE);
    }

    @Test
    void returnsEmptyWhenTheUserDoesNotExist() {
        when(userRepository.findByExternalId(USER_ID)).thenReturn(Optional.empty());

        assertThat(useCase.startReservation(USER_ID, 15L, DAY, NINE_THIRTY)).isEmpty();
        verifyNoInteractions(catedraRepository);
        assertThat(processes.savedStatuses).isEmpty();
    }

    @Test
    void rejectsASlotThatAlreadyStarted() {
        givenTheUser();

        // 2099-09-10 14:30 UTC is 11:30 in Buenos Aires.
        assertThatThrownBy(() -> useCase.startReservation(USER_ID, 15L, LocalDate.of(2099, 9, 10), LocalTime.of(11, 0)))
                .isInstanceOf(SlotInThePastException.class);
        verifyNoInteractions(catedraRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PROFESSIONAL_NOT_FOUND", "PROFESSIONAL_DISABLED", "INVALID_SLOT",
            "SLOT_ALREADY_RESERVED", "SLOT_ALREADY_HELD"})
    void failsWithTheCodeOfTheCatedraWhenItRejectsTheHold(String code) {
        givenTheUser();
        when(catedraRepository.createHold(15L, DAY, NINE_THIRTY, USER_ID)).thenThrow(rejection(code));

        assertThatStartingFails().isInstanceOfSatisfying(ReservationRejectedException.class, e ->
                assertThat(e.getCode()).isEqualTo(code));
        assertTheProcessFailedWith(code);
        verify(catedraRepository, never()).confirmHold(anyString(), anyString(), anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PROFESSIONAL_NOT_FOUND", "INVALID_SLOT", "SLOT_ALREADY_RESERVED", "HOLD_NOT_FOUND"})
    void failsWithTheCodeOfTheCatedraWhenItRejectsTheConfirmation(String code) {
        givenAHoldWhoseConfirmationFailsWith(rejection(code));

        assertThatStartingFails().isInstanceOfSatisfying(ReservationRejectedException.class, e ->
                assertThat(e.getCode()).isEqualTo(code));
        assertTheProcessFailedWith(code);
    }

    @Test
    void expiresWhenTheCatedraSaysTheHoldExpired() {
        givenAHoldWhoseConfirmationFailsWith(rejection("HOLD_EXPIRED"));

        assertThatStartingFails().isInstanceOf(HoldExpiredException.class);
        assertThat(processes.only().getStatus()).isEqualTo(EXPIRED);
    }

    @Test
    void waitsForThePhoneWhenAnEarlierConfirmationOfTheProcessArrived() {
        givenAHoldWhoseConfirmationFailsWith(rejection("RESERVATION_PROCESS_ALREADY_CONFIRMED"));

        ReservationProcess process = start().process();

        assertThat(process.getStatus()).isEqualTo(WAITING_FOR_PHONE);
        assertThat(process.getExpiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    void isUnavailableWhenTheCatedraDoesNotFindTheTechnicalIntegration() {
        givenTheUser();
        when(catedraRepository.createHold(15L, DAY, NINE_THIRTY, USER_ID))
                .thenThrow(new CatedraRequestRejectedException(403, "STUDENT_INTEGRATION_NOT_FOUND", "no", null));

        assertThatStartingFails().isInstanceOf(ReservationUnavailableException.class);
        assertTheProcessFailedWith("STUDENT_INTEGRATION_NOT_FOUND");
    }

    @ParameterizedTest
    @ValueSource(strings = {"VALIDATION_ERROR", "HOLD_OWNER_MISMATCH", "EXTERNAL_PATIENT_MISMATCH"})
    void failsAsUnexpectedWithACodeThisServiceShouldNeverCause(String code) {
        givenAHoldWhoseConfirmationFailsWith(rejection(code));

        assertThatStartingFails().isInstanceOf(UnexpectedCatedraRejectionException.class);
        assertTheProcessFailedWith(code);
    }

    @Test
    void requestsTheHoldAgainWhenTheSlotIsStillFree() {
        givenTheUser();
        when(catedraRepository.createHold(15L, DAY, NINE_THIRTY, USER_ID)).thenThrow(unavailable()).thenReturn(HOLD);
        givenTheOccupancies(new Occupancy(DAY, NINE_THIRTY.plusMinutes(30), OccupancyStatus.HELD, EXPIRES_AT));
        givenTheConfirmation();

        assertThat(start().process().getStatus()).isEqualTo(WAITING_FOR_PHONE);
        verify(catedraRepository, times(2)).createHold(15L, DAY, NINE_THIRTY, USER_ID);
    }

    @Test
    void staysPendingTheHoldWhenTheSlotIsHeldBecauseTheHoldMayBeOurs() {
        givenTheUser();
        when(catedraRepository.createHold(15L, DAY, NINE_THIRTY, USER_ID)).thenThrow(unavailable());
        givenTheOccupancies(new Occupancy(DAY, NINE_THIRTY, OccupancyStatus.HELD, EXPIRES_AT));

        assertThatStartingFails().isInstanceOfSatisfying(ReservationPendingException.class, e ->
                assertThat(e.getId()).isEqualTo(processes.only().getId()));
        assertThat(processes.only().getStatus()).isEqualTo(PENDING_HOLD);
        verify(catedraRepository, times(1)).createHold(any(), any(), any(), any());
    }

    @Test
    void failsAsReservedWhenTheSlotOfAHoldWithoutAnswerIsConfirmed() {
        givenTheUser();
        when(catedraRepository.createHold(15L, DAY, NINE_THIRTY, USER_ID)).thenThrow(unavailable());
        givenTheOccupancies(new Occupancy(DAY, NINE_THIRTY, OccupancyStatus.CONFIRMED, null));

        assertThatStartingFails().isInstanceOfSatisfying(ReservationRejectedException.class, e ->
                assertThat(e.getCode()).isEqualTo("SLOT_ALREADY_RESERVED"));
        assertTheProcessFailedWith("SLOT_ALREADY_RESERVED");
    }

    @Test
    void staysPendingTheHoldWhenTheOccupanciesCannotBeRead() {
        givenTheUser();
        when(catedraRepository.createHold(15L, DAY, NINE_THIRTY, USER_ID)).thenThrow(unavailable());
        when(catedraRepository.findOccupancies(15L, DAY, DAY)).thenThrow(unavailable());

        assertThatStartingFails().isInstanceOf(ReservationPendingException.class);
        assertThat(processes.only().getStatus()).isEqualTo(PENDING_HOLD);
        verify(catedraRepository, times(1)).createHold(any(), any(), any(), any());
    }

    @Test
    void requestsTheHoldAtMostTwiceInARequest() {
        givenTheUser();
        when(catedraRepository.createHold(15L, DAY, NINE_THIRTY, USER_ID)).thenThrow(unavailable());

        assertThatStartingFails().isInstanceOf(ReservationPendingException.class);
        assertThat(processes.only().getStatus()).isEqualTo(PENDING_HOLD);
        verify(catedraRepository, times(StartReservationUseCaseImpl.MAX_ATTEMPTS))
                .createHold(any(), any(), any(), any());
    }

    @Test
    void doesNotConfirmAgainWhenTheReservationAlreadyExists() {
        givenAHoldWhoseConfirmationFailsWith(unavailable());
        givenTheAppointments(appointment("process-a92b47d0", AppointmentStatus.PHONE_PENDING));

        ReservationProcess process = start().process();

        assertThat(process.getStatus()).isEqualTo(WAITING_FOR_PHONE);
        assertThat(process.getExpiresAt()).isEqualTo(EXPIRES_AT);
        verify(catedraRepository, times(1)).confirmHold(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void confirmsAgainWhenTheReservationDoesNotExist() {
        givenTheUser();
        when(catedraRepository.createHold(15L, DAY, NINE_THIRTY, USER_ID)).thenReturn(HOLD);
        when(catedraRepository.confirmHold(HOLD_ID, USER_ID, "Juan", "Perez")).thenThrow(unavailable())
                .thenReturn(new HoldConfirmation("process-a92b47d0", EXPIRES_AT.plusSeconds(60)));
        // A reservation of another process of the same patient and day is not this one.
        givenTheAppointments(appointment("process-other", AppointmentStatus.CONFIRMED));

        assertThat(start().process().getExpiresAt()).isEqualTo(EXPIRES_AT.plusSeconds(60));
        verify(catedraRepository, times(2)).confirmHold(HOLD_ID, USER_ID, "Juan", "Perez");
    }

    @Test
    void waitsForThePhoneWhenTheRepeatedConfirmationFindsItAlreadyConfirmed() {
        givenTheUser();
        when(catedraRepository.createHold(15L, DAY, NINE_THIRTY, USER_ID)).thenReturn(HOLD);
        when(catedraRepository.confirmHold(HOLD_ID, USER_ID, "Juan", "Perez")).thenThrow(unavailable())
                .thenThrow(rejection("RESERVATION_PROCESS_ALREADY_CONFIRMED"));

        assertThat(start().process().getStatus()).isEqualTo(WAITING_FOR_PHONE);
    }

    @Test
    void staysConfirmingWhenTheReservationsCannotBeRead() {
        givenAHoldWhoseConfirmationFailsWith(unavailable());
        when(catedraRepository.findAppointments(USER_ID, DAY, DAY)).thenThrow(unavailable());

        assertThatStartingFails().isInstanceOf(ReservationPendingException.class);
        assertThat(processes.only().getStatus()).isEqualTo(CONFIRMING);
        verify(catedraRepository, times(1)).confirmHold(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void confirmsAtMostTwiceInARequest() {
        givenAHoldWhoseConfirmationFailsWith(unavailable());

        assertThatStartingFails().isInstanceOf(ReservationPendingException.class);
        assertThat(processes.only().getStatus()).isEqualTo(CONFIRMING);
        verify(catedraRepository, times(StartReservationUseCaseImpl.MAX_ATTEMPTS))
                .confirmHold(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void returnsTheActiveProcessOfTheSlotWhenItAlreadyWaitsForThePhone() {
        givenTheUser();
        ReservationProcess existing = processes.storedIn(WAITING_FOR_PHONE, EXPIRES_AT);

        StartedReservation started = start();

        assertThat(started.resumed()).isTrue();
        assertThat(started.process()).isSameAs(existing);
        verifyNoInteractions(catedraRepository);
    }

    @Test
    void confirmsAHeldProcessOfTheSlotWithoutHoldingItAgain() {
        givenTheUser();
        processes.storedIn(HELD, EXPIRES_AT);
        when(catedraRepository.confirmHold(HOLD_ID, USER_ID, "Juan", "Perez"))
                .thenReturn(new HoldConfirmation("process-a92b47d0", EXPIRES_AT));

        StartedReservation started = start();

        assertThat(started.resumed()).isTrue();
        assertThat(started.process().getStatus()).isEqualTo(WAITING_FOR_PHONE);
        verify(catedraRepository, never()).createHold(any(), any(), any(), any());
    }

    @Test
    void expiresAHeldProcessOfTheSlotWhoseHoldAlreadyExpired() {
        givenTheUser();
        processes.storedIn(HELD, NOW);

        assertThatStartingFails().isInstanceOf(HoldExpiredException.class);
        assertThat(processes.only().getStatus()).isEqualTo(EXPIRED);
        verifyNoInteractions(catedraRepository);
    }

    @Test
    void resumesAPendingHoldByCheckingTheSlotFirst() {
        givenTheUser();
        processes.storedIn(PENDING_HOLD, null);
        when(catedraRepository.createHold(15L, DAY, NINE_THIRTY, USER_ID)).thenReturn(HOLD);
        givenTheConfirmation();

        StartedReservation started = start();

        assertThat(started.resumed()).isTrue();
        assertThat(started.process().getStatus()).isEqualTo(WAITING_FOR_PHONE);
        verify(catedraRepository).findOccupancies(15L, DAY, DAY);
    }

    @Test
    void doesNotRequestAgainAPendingHoldWhoseSlotIsHeld() {
        givenTheUser();
        processes.storedIn(PENDING_HOLD, null);
        givenTheOccupancies(new Occupancy(DAY, NINE_THIRTY, OccupancyStatus.HELD, EXPIRES_AT));

        assertThatStartingFails().isInstanceOf(ReservationPendingException.class);
        verify(catedraRepository, never()).createHold(any(), any(), any(), any());
    }

    @Test
    void resumesAConfirmationThatReachedTheCatedraWithoutConfirmingAgain() {
        givenTheUser();
        processes.storedIn(CONFIRMING, EXPIRES_AT);
        givenTheAppointments(appointment("process-a92b47d0", AppointmentStatus.PHONE_PENDING));

        assertThat(start().process().getStatus()).isEqualTo(WAITING_FOR_PHONE);
        verify(catedraRepository, never()).confirmHold(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void expiresAConfirmingProcessWhoseHoldAlreadyExpiredWithoutCallingTheCatedra() {
        givenTheUser();
        processes.storedIn(CONFIRMING, NOW);

        assertThatStartingFails().isInstanceOf(HoldExpiredException.class);
        assertThat(processes.only().getStatus()).isEqualTo(EXPIRED);
        verifyNoInteractions(catedraRepository);
    }

    private void givenTheUser() {
        when(userRepository.findByExternalId(USER_ID)).thenReturn(Optional.of(USER));
    }

    private void givenTheConfirmation() {
        when(catedraRepository.confirmHold(HOLD_ID, USER_ID, "Juan", "Perez"))
                .thenReturn(new HoldConfirmation("process-a92b47d0", EXPIRES_AT));
    }

    private void givenTheOccupancies(Occupancy... occupancies) {
        when(catedraRepository.findOccupancies(15L, DAY, DAY)).thenReturn(List.of(occupancies));
    }

    private void givenTheAppointments(Appointment... appointments) {
        when(catedraRepository.findAppointments(USER_ID, DAY, DAY)).thenReturn(List.of(appointments));
    }

    private static Appointment appointment(String reservationProcessId, AppointmentStatus status) {
        return new Appointment(8251L, reservationProcessId, USER_ID, 15L, DAY, NINE_THIRTY, status);
    }

    private void givenAHoldWhoseConfirmationFailsWith(RuntimeException failure) {
        givenTheUser();
        when(catedraRepository.createHold(15L, DAY, NINE_THIRTY, USER_ID)).thenReturn(HOLD);
        when(catedraRepository.confirmHold(HOLD_ID, USER_ID, "Juan", "Perez")).thenThrow(failure);
    }

    private StartedReservation start() {
        return useCase.startReservation(USER_ID, 15L, DAY, NINE_THIRTY).orElseThrow();
    }

    private AbstractThrowableAssert<?, ? extends Throwable> assertThatStartingFails() {
        return assertThatThrownBy(() -> useCase.startReservation(USER_ID, 15L, DAY, NINE_THIRTY));
    }

    private void assertTheProcessFailedWith(String code) {
        assertThat(processes.only().getStatus()).isEqualTo(FAILED);
        assertThat(processes.only().getFailureCode()).isEqualTo(code);
    }

    private static CatedraRequestRejectedException rejection(String code) {
        return new CatedraRequestRejectedException(400, code, "rejected", null);
    }

    private static CatedraUnavailableException unavailable() {
        return new CatedraUnavailableException("timeout", null);
    }

    /**
     * Keeps the processes in memory and records the status of every create and update, in order.
     */
    private static final class InMemoryReservationProcessRepository implements ReservationProcessRepository {

        private final Map<Long, ReservationProcess> byId = new HashMap<>();
        private final List<ReservationStatus> savedStatuses = new ArrayList<>();

        @Override
        public ReservationProcess create(ReservationProcess process) {
            ReservationProcess created = copyOf(process, (long) byId.size() + 1, process.getStatus(),
                    process.getExpiresAt());
            byId.put(created.getId(), created);
            savedStatuses.add(created.getStatus());
            return created;
        }

        @Override
        public Optional<ReservationProcess> update(Long id, ReservationProcess process) {
            byId.put(id, process);
            savedStatuses.add(process.getStatus());
            return Optional.of(process);
        }

        @Override
        public Optional<ReservationProcess> findByIdAndUserExternalId(Long id, String userExternalId) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<ReservationProcess> findActiveByUserAndSlot(String userExternalId, Long professionalId,
                                                                    LocalDate date, LocalTime startTime) {
            return byId.values().stream().filter(ReservationProcess::isActive).findFirst();
        }

        ReservationProcess storedIn(ReservationStatus status, Instant expiresAt) {
            ReservationProcess process = copyOf(ReservationProcess.start(USER_ID, 15L, DAY, NINE_THIRTY, NOW),
                    (long) byId.size() + 1, status, status == PENDING_HOLD ? null : expiresAt);
            byId.put(process.getId(), process);
            return process;
        }

        ReservationProcess only() {
            assertThat(byId).hasSize(1);
            return byId.values().iterator().next();
        }

        private static ReservationProcess copyOf(ReservationProcess process, Long id, ReservationStatus status,
                                                 Instant expiresAt) {
            boolean held = status != PENDING_HOLD;
            return new ReservationProcess(id, process.getUserExternalId(), process.getProfessionalId(),
                    process.getDate(), process.getStartTime(), status, held ? HOLD_ID : null,
                    held ? "process-a92b47d0" : null, expiresAt, null, process.getCreatedAt(), NOW, 0L);
        }
    }
}
