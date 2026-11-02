package com.toneatti.turnos.reservation.application.usecases;

import com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException;
import com.toneatti.turnos.catedra.domain.exception.CatedraUnavailableException;
import com.toneatti.turnos.catedra.domain.model.AppointmentPage;
import com.toneatti.turnos.catedra.domain.model.AppointmentStatus;
import com.toneatti.turnos.catedra.domain.ports.out.CatedraRepository;
import com.toneatti.turnos.reservation.application.exception.InvalidDateRangeException;
import com.toneatti.turnos.reservation.application.exception.ReservationUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListReservationsUseCaseImplTest {

    private static final String USER_ID = "user-42";
    private static final LocalDate DAY = LocalDate.of(2026, 9, 14);

    @Mock
    private CatedraRepository catedraRepository;

    @InjectMocks
    private ListReservationsUseCaseImpl useCase;

    @Test
    void returnsThePageOfTheCatedraWithEveryFilter() {
        AppointmentPage page = new AppointmentPage(List.of(), 37);
        when(catedraRepository.findAppointmentsPage(USER_ID, DAY, DAY, AppointmentStatus.CONFIRMED, 0, 20))
                .thenReturn(page);

        assertThat(useCase.listReservations(USER_ID, DAY, DAY, AppointmentStatus.CONFIRMED, 0, 20)).isSameAs(page);
    }

    @Test
    void acceptsAMissingDateRange() {
        AppointmentPage page = new AppointmentPage(List.of(), 0);
        when(catedraRepository.findAppointmentsPage(USER_ID, null, null, null, 0, 20)).thenReturn(page);

        assertThat(useCase.listReservations(USER_ID, null, null, null, 0, 20)).isSameAs(page);
    }

    @Test
    void failsWithInvalidDateRangeWhenFromIsAfterTo() {
        assertThatThrownBy(() -> useCase.listReservations(USER_ID, DAY.plusDays(1), DAY, null, 0, 20))
                .isInstanceOf(InvalidDateRangeException.class);

        verify(catedraRepository, never())
                .findAppointmentsPage(anyString(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void failsWithInvalidDateRangeWhenTheCatedraRejectsIt() {
        when(catedraRepository.findAppointmentsPage(USER_ID, DAY, DAY, null, 0, 20))
                .thenThrow(new CatedraRequestRejectedException(400,
                        CatedraRequestRejectedException.INVALID_DATE_RANGE, "rejected", null));

        assertThatThrownBy(() -> useCase.listReservations(USER_ID, DAY, DAY, null, 0, 20))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    @Test
    void failsAsUnavailableWhenTheIntegrationIsNotFound() {
        when(catedraRepository.findAppointmentsPage(USER_ID, DAY, DAY, null, 0, 20))
                .thenThrow(new CatedraRequestRejectedException(403,
                        CatedraRequestRejectedException.STUDENT_INTEGRATION_NOT_FOUND, "rejected", null));

        assertThatThrownBy(() -> useCase.listReservations(USER_ID, DAY, DAY, null, 0, 20))
                .isInstanceOf(ReservationUnavailableException.class);
    }

    @Test
    void failsAsUnavailableWhenTheCatedraDoesNotAnswer() {
        when(catedraRepository.findAppointmentsPage(USER_ID, DAY, DAY, null, 0, 20))
                .thenThrow(new CatedraUnavailableException("no answer", null));

        assertThatThrownBy(() -> useCase.listReservations(USER_ID, DAY, DAY, null, 0, 20))
                .isInstanceOf(ReservationUnavailableException.class);
    }
}
