package com.toneatti.turnos.reservation.infrastructure.persistence.mapper;

import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.model.ReservationStatus;
import com.toneatti.turnos.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationProcessMapperTest {

    private final ReservationProcessMapper reservationProcessMapper = new ReservationProcessMapper();

    @Test
    void returnsNullForNullInput() {
        assertThat(reservationProcessMapper.toEntity(null)).isNull();
        assertThat(reservationProcessMapper.toDomainModel(null)).isNull();
    }

    @Test
    void mapsEveryFieldBothWays() {
        ReservationProcess process = processIn(ReservationStatus.WAITING_FOR_PHONE);

        ReservationProcessEntity entity = reservationProcessMapper.toEntity(process);

        assertThat(entity).usingRecursiveComparison().ignoringFields("active").isEqualTo(process);
        assertThat(reservationProcessMapper.toDomainModel(entity)).usingRecursiveComparison().isEqualTo(process);
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"CANCELLED", "EXPIRED", "FAILED"},
            mode = EnumSource.Mode.EXCLUDE)
    void marksAnActiveProcessAsActive(ReservationStatus status) {
        assertThat(reservationProcessMapper.toEntity(processIn(status)).getActive()).isTrue();
    }

    // NULL, never FALSE: see the unique constraint of ReservationProcessEntity.
    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"CANCELLED", "EXPIRED", "FAILED"})
    void leavesAFinishedProcessWithoutActive(ReservationStatus status) {
        assertThat(reservationProcessMapper.toEntity(processIn(status)).getActive()).isNull();
    }

    private static ReservationProcess processIn(ReservationStatus status) {
        return new ReservationProcess(7L, "user-42", 15L, LocalDate.of(2099, 9, 14), LocalTime.of(9, 30), status,
                "hold-6c0c2f41", "process-a92b47d0", Instant.parse("2099-09-10T14:44:50Z"), "SLOT_ALREADY_HELD",
                "request-event-8a31", 8251L, "INVALID_PHONE_NUMBER", "Cambio solicitado por el usuario",
                Instant.parse("2099-09-10T14:30:00Z"), Instant.parse("2099-09-10T14:30:05Z"), 3L);
    }
}
