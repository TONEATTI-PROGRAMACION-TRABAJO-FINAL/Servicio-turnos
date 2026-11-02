package com.toneatti.turnos.reservation.application.usecases;

import com.toneatti.turnos.reservation.domain.model.AppointmentActionOutcome;
import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.model.ReservationStatus;
import com.toneatti.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import com.toneatti.turnos.reservation.infrastructure.persistence.adapter.JpaProcessedAppointmentActionRepositoryAdapter;
import com.toneatti.turnos.reservation.infrastructure.persistence.repository.JpaProcessedAppointmentActionRepository;
import com.toneatti.turnos.reservation.infrastructure.persistence.repository.JpaReservationProcessRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static com.toneatti.turnos.reservation.domain.model.AppointmentActionFixtures.CANCELLATION;
import static com.toneatti.turnos.reservation.domain.model.AppointmentActionFixtures.CONFIRMATION;
import static com.toneatti.turnos.reservation.domain.model.AppointmentActionFixtures.PROCESS_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

/**
 * The transaction of the use case against H2: the change of the process and the record of the eventId are saved
 * together or not at all.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplyAppointmentActionUseCaseImplIntegrationTest {

    private static final Instant NOW = Instant.parse("2099-09-10T14:30:00Z");

    @Autowired
    private ApplyAppointmentActionUseCaseImpl useCase;

    @Autowired
    private ReservationProcessRepository reservationProcessRepository;

    @Autowired
    private JpaReservationProcessRepository jpaReservationProcessRepository;

    @Autowired
    private JpaProcessedAppointmentActionRepository jpaProcessedAppointmentActionRepository;

    @MockitoSpyBean
    private JpaProcessedAppointmentActionRepositoryAdapter processedAppointmentActionRepository;

    @BeforeEach
    void givenAProcessWaitingForThePhone() {
        ReservationProcess process = reservationProcessRepository.create(
                ReservationProcess.start("user-42", 15L, LocalDate.of(2099, 9, 14), LocalTime.of(9, 30), NOW));
        process.markHeld("hold-6c0c2f41", PROCESS_ID, NOW.plusSeconds(890), NOW);
        process.markConfirming(NOW);
        process.markWaitingForPhone(null, NOW);
        reservationProcessRepository.update(process.getId(), process);
    }

    @AfterEach
    void deleteAll() {
        jpaProcessedAppointmentActionRepository.deleteAll();
        jpaReservationProcessRepository.deleteAll();
    }

    @Test
    void appliesAnEventOnceAndIgnoresItsRedelivery() {
        assertThat(useCase.applyAppointmentAction(CONFIRMATION)).isEqualTo(AppointmentActionOutcome.APPLIED);
        assertThat(useCase.applyAppointmentAction(CONFIRMATION)).isEqualTo(AppointmentActionOutcome.DUPLICATE);

        assertThat(storedStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(jpaProcessedAppointmentActionRepository.count()).isEqualTo(1);
    }

    // Another consumer recorded the event between the check and the insert: the change of the process is undone.
    @Test
    void rollsTheProcessBackWhenTheEventWasRecordedMeanwhile() {
        useCase.applyAppointmentAction(CONFIRMATION);
        jpaReservationProcessRepository.deleteAll();
        givenAProcessWaitingForThePhone();
        doReturn(false).when(processedAppointmentActionRepository).existsByEventId(CONFIRMATION.eventId());

        assertThatThrownBy(() -> useCase.applyAppointmentAction(CONFIRMATION))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(storedStatus()).isEqualTo(ReservationStatus.WAITING_FOR_PHONE);
        assertThat(jpaProcessedAppointmentActionRepository.count()).isEqualTo(1);
    }

    // The user cancelled through REST before the event of that cancellation arrived: the event changes nothing,
    // and its redelivery is recognized as a duplicate.
    @Test
    void ignoresTheCancellationEventOfAReservationAlreadyCancelledThroughRest() {
        useCase.applyAppointmentAction(CONFIRMATION);
        ReservationProcess process = reservationProcessRepository.findByReservationProcessId(PROCESS_ID).orElseThrow();
        process.markCancelled("Cambio solicitado por el usuario", NOW);
        reservationProcessRepository.update(process.getId(), process);

        assertThat(useCase.applyAppointmentAction(CANCELLATION)).isEqualTo(AppointmentActionOutcome.LATE);
        assertThat(useCase.applyAppointmentAction(CANCELLATION)).isEqualTo(AppointmentActionOutcome.DUPLICATE);

        assertThat(storedStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(jpaProcessedAppointmentActionRepository.count()).isEqualTo(2);
    }

    private ReservationStatus storedStatus() {
        return reservationProcessRepository.findByReservationProcessId(PROCESS_ID).orElseThrow().getStatus();
    }
}
