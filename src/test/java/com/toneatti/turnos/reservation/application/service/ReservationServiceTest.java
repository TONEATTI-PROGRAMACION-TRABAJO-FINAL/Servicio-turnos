package com.toneatti.turnos.reservation.application.service;

import com.toneatti.turnos.reservation.application.exception.ReservationException;
import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.model.StartedReservation;
import com.toneatti.turnos.reservation.domain.ports.in.StartReservationUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final LocalDate DAY = LocalDate.of(2099, 9, 14);
    private static final LocalTime NINE_THIRTY = LocalTime.of(9, 30);

    @Mock
    private StartReservationUseCase startReservationUseCase;

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
}
