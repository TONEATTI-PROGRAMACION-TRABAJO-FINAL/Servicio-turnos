package com.toneatti.turnos.reservation.infrastructure.persistence.adapter;

import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import com.toneatti.turnos.reservation.infrastructure.persistence.mapper.ReservationProcessMapper;
import com.toneatti.turnos.reservation.infrastructure.persistence.repository.JpaReservationProcessRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The null cases the H2 tests of {@link JpaReservationProcessRepositoryAdapterIntegrationTest} cannot produce.
 */
@ExtendWith(MockitoExtension.class)
class JpaReservationProcessRepositoryAdapterTest {

    @Mock
    private JpaReservationProcessRepository jpaReservationProcessRepository;

    private JpaReservationProcessRepositoryAdapter adapter() {
        return new JpaReservationProcessRepositoryAdapter(jpaReservationProcessRepository,
                new ReservationProcessMapper());
    }

    @Test
    void rejectsUpdatingANullProcessBeforeTouchingTheDatabase() {
        assertThatThrownBy(() -> adapter().update(7L, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("process");
        verifyNoInteractions(jpaReservationProcessRepository);
    }

    // Spring Data never does this; if it did, the caller must not get a null process.
    @Test
    void failsInsteadOfReturningANullProcessWhenTheSaveReturnsNothing() {
        when(jpaReservationProcessRepository.saveAndFlush(any(ReservationProcessEntity.class))).thenReturn(null);

        assertThatThrownBy(() -> adapter().create(ReservationProcess.start("user-42", 15L,
                LocalDate.of(2099, 9, 14), LocalTime.of(9, 30), Instant.parse("2099-09-10T14:30:00Z"))))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("saveAndFlush returned null");
    }
}
