package com.toneatti.turnos.reservation.application.usecases;

import com.toneatti.turnos.reservation.application.exception.ConcurrentReservationUpdateException;
import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.model.ReservationStatus;
import com.toneatti.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetReservationByIdUseCaseImplTest {

    private static final Instant NOW = Instant.parse("2099-09-10T14:30:00Z");
    private static final String USER_ID = "user-42";

    @Mock
    private ReservationProcessRepository reservationProcessRepository;

    private GetReservationByIdUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetReservationByIdUseCaseImpl(reservationProcessRepository,
                Clock.fixed(NOW, ZoneId.of("America/Argentina/Buenos_Aires")));
    }

    @Test
    void returnsTheProcessOfTheUser() {
        ReservationProcess process = processIn(ReservationStatus.WAITING_FOR_PHONE, NOW.plusSeconds(600));
        when(reservationProcessRepository.findByIdAndUserExternalId(7L, USER_ID)).thenReturn(Optional.of(process));

        assertThat(useCase.getReservationById(USER_ID, 7L)).containsSame(process);
        verify(reservationProcessRepository, never()).update(anyLong(), any());
    }

    @Test
    void returnsEmptyWhenTheProcessDoesNotExistOrBelongsToAnotherUser() {
        when(reservationProcessRepository.findByIdAndUserExternalId(7L, USER_ID)).thenReturn(Optional.empty());

        assertThat(useCase.getReservationById(USER_ID, 7L)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"HELD", "CONFIRMING"})
    void expiresAProcessWhoseHoldExpired(ReservationStatus status) {
        givenTheStoredProcess(processIn(status, NOW));
        when(reservationProcessRepository.update(any(), any()))
                .thenAnswer(invocation -> Optional.of(invocation.getArgument(1)));

        assertThat(useCase.getReservationById(USER_ID, 7L))
                .hasValueSatisfying(found -> assertThat(found.getStatus()).isEqualTo(ReservationStatus.EXPIRED));
    }

    // The end of the phone step comes from the cátedra (Kafka), not from the local clock.
    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"PENDING_HOLD", "WAITING_FOR_PHONE", "FAILED", "EXPIRED"})
    void leavesTheOtherStatusesAsTheyAre(ReservationStatus status) {
        givenTheStoredProcess(processIn(status, NOW.minusSeconds(60)));

        assertThat(useCase.getReservationById(USER_ID, 7L))
                .hasValueSatisfying(found -> assertThat(found.getStatus()).isEqualTo(status));
        verify(reservationProcessRepository, never()).update(anyLong(), any());
    }

    @Test
    void returnsTheCurrentVersionWhenAnotherRequestChangedTheProcess() {
        ReservationProcess current = processIn(ReservationStatus.FAILED, NOW);
        when(reservationProcessRepository.findByIdAndUserExternalId(7L, USER_ID))
                .thenReturn(Optional.of(processIn(ReservationStatus.HELD, NOW)), Optional.of(current));
        when(reservationProcessRepository.update(any(), any()))
                .thenThrow(new ConcurrentReservationUpdateException(7L, null));

        assertThat(useCase.getReservationById(USER_ID, 7L)).containsSame(current);
    }

    private void givenTheStoredProcess(ReservationProcess process) {
        when(reservationProcessRepository.findByIdAndUserExternalId(7L, USER_ID)).thenReturn(Optional.of(process));
    }

    private static ReservationProcess processIn(ReservationStatus status, Instant expiresAt) {
        return new ReservationProcess(7L, USER_ID, 15L, LocalDate.of(2099, 9, 14), LocalTime.of(9, 30), status,
                "hold-6c0c2f41", "process-a92b47d0", expiresAt, null, null, null, null, null, NOW, NOW, 0L);
    }
}
