package com.toneatti.turnos.reservation.application.service;

import com.toneatti.turnos.catedra.domain.model.AppointmentPage;
import com.toneatti.turnos.catedra.domain.model.AppointmentStatus;
import com.toneatti.turnos.reservation.application.exception.ReservationException;
import com.toneatti.turnos.reservation.domain.model.AppointmentActionOutcome;
import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.model.StartedReservation;
import com.toneatti.turnos.reservation.domain.ports.in.ApplyAppointmentActionUseCase;
import com.toneatti.turnos.reservation.domain.ports.in.CancelReservationUseCase;
import com.toneatti.turnos.reservation.domain.ports.in.GetReservationByIdUseCase;
import com.toneatti.turnos.reservation.domain.ports.in.ListReservationsUseCase;
import com.toneatti.turnos.reservation.domain.ports.in.StartReservationUseCase;
import com.toneatti.turnos.reservation.domain.ports.in.SubmitPhoneUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.toneatti.turnos.reservation.domain.model.AppointmentActionFixtures.CONFIRMATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final LocalDate DAY = LocalDate.of(2099, 9, 14);
    private static final LocalTime NINE_THIRTY = LocalTime.of(9, 30);

    @Mock
    private StartReservationUseCase startReservationUseCase;

    @Mock
    private GetReservationByIdUseCase getReservationByIdUseCase;

    @Mock
    private ApplyAppointmentActionUseCase applyAppointmentActionUseCase;

    @Mock
    private SubmitPhoneUseCase submitPhoneUseCase;

    @Mock
    private ListReservationsUseCase listReservationsUseCase;

    @Mock
    private CancelReservationUseCase cancelReservationUseCase;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void startReservationReturnsTheReservationOfTheUseCase() {
        StartedReservation started = new StartedReservation(
                ReservationProcess.start("user-42", 15L, DAY, NINE_THIRTY, Instant.now()), false);
        when(startReservationUseCase.startReservation("user-42", 15L, DAY, NINE_THIRTY))
                .thenReturn(Optional.of(started));

        assertThat(reservationService.startReservation("user-42", 15L, DAY, NINE_THIRTY)).isSameAs(started);
    }

    @Test
    void startReservationThrowsReservationExceptionWhenTheUserDoesNotExist() {
        when(startReservationUseCase.startReservation("missing", 15L, DAY, NINE_THIRTY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.startReservation("missing", 15L, DAY, NINE_THIRTY))
                .isInstanceOf(ReservationException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void getReservationByIdReturnsTheProcessOfTheUseCase() {
        ReservationProcess process = ReservationProcess.start("user-42", 15L, DAY, NINE_THIRTY, Instant.now());
        when(getReservationByIdUseCase.getReservationById("user-42", 7L)).thenReturn(Optional.of(process));

        assertThat(reservationService.getReservationById("user-42", 7L)).isSameAs(process);
    }

    @Test
    void getReservationByIdThrowsReservationExceptionWhenItIsNotFound() {
        when(getReservationByIdUseCase.getReservationById("user-42", 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.getReservationById("user-42", 99L))
                .isInstanceOf(ReservationException.class)
                .hasMessageContaining("99");
    }

    @Test
    void submitPhoneReturnsTheProcessOfTheUseCase() {
        ReservationProcess process = ReservationProcess.start("user-42", 15L, DAY, NINE_THIRTY, Instant.now());
        when(submitPhoneUseCase.submitPhone("user-42", 7L, "+5492615551234")).thenReturn(Optional.of(process));

        assertThat(reservationService.submitPhone("user-42", 7L, "+5492615551234")).isSameAs(process);
    }

    @Test
    void submitPhoneThrowsReservationExceptionWhenItIsNotFound() {
        when(submitPhoneUseCase.submitPhone("user-42", 99L, "+5492615551234")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.submitPhone("user-42", 99L, "+5492615551234"))
                .isInstanceOf(ReservationException.class)
                .hasMessageContaining("99");
    }

    @Test
    void applyAppointmentActionReturnsTheOutcomeOfTheUseCase() {
        when(applyAppointmentActionUseCase.applyAppointmentAction(CONFIRMATION))
                .thenReturn(AppointmentActionOutcome.APPLIED);

        assertThat(reservationService.applyAppointmentAction(CONFIRMATION)).isEqualTo(AppointmentActionOutcome.APPLIED);
    }

    @Test
    void listReservationsReturnsThePageOfTheUseCase() {
        AppointmentPage page = new AppointmentPage(List.of(), 0);
        when(listReservationsUseCase.listReservations("user-42", DAY, DAY, AppointmentStatus.CONFIRMED, 0, 20))
                .thenReturn(page);

        assertThat(reservationService.listReservations("user-42", DAY, DAY, AppointmentStatus.CONFIRMED, 0, 20))
                .isSameAs(page);
    }

    @Test
    void cancelReservationReturnsTheProcessOfTheUseCase() {
        ReservationProcess process = ReservationProcess.start("user-42", 15L, DAY, NINE_THIRTY, Instant.now());
        when(cancelReservationUseCase.cancelReservation("user-42", "process-a92b47d0", null))
                .thenReturn(Optional.of(process));

        assertThat(reservationService.cancelReservation("user-42", "process-a92b47d0", null)).isSameAs(process);
    }

    @Test
    void cancelReservationThrowsReservationExceptionWhenItIsNotFound() {
        when(cancelReservationUseCase.cancelReservation("user-42", "process-other", null))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.cancelReservation("user-42", "process-other", null))
                .isInstanceOf(ReservationException.class)
                .hasMessageContaining("process-other");
    }
}
