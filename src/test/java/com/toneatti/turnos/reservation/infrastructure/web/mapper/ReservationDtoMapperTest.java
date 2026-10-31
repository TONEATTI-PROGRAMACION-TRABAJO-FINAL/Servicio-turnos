package com.toneatti.turnos.reservation.infrastructure.web.mapper;

import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.model.ReservationStatus;
import com.toneatti.turnos.reservation.infrastructure.web.dto.ReservationResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationDtoMapperTest {

    private final ReservationDtoMapper reservationDtoMapper = new ReservationDtoMapper();

    @Test
    void returnsNullForNullInput() {
        assertThat(reservationDtoMapper.toResponse(null)).isNull();
    }

    // Neither the ids of the cátedra (holdId, reservationProcessId, requestEventId, its reservationId) are exposed.
    @Test
    void mapsTheProcessAndTheOutcomeOfTheCatedraWithoutItsIds() {
        ReservationProcess process = new ReservationProcess(7L, "user-42", 15L, LocalDate.of(2099, 9, 14),
                LocalTime.of(9, 30), ReservationStatus.CANCELLED, "hold-6c0c2f41", "process-a92b47d0",
                Instant.parse("2099-09-10T14:44:50Z"), "HOLD_EXPIRED", "request-event-8a31", 8251L,
                "INVALID_PHONE_NUMBER", "Cambio solicitado por el usuario",
                Instant.parse("2099-09-10T14:30:00Z"), Instant.parse("2099-09-10T14:30:05Z"), 3L);

        ReservationResponse response = reservationDtoMapper.toResponse(process);

        assertThat(response).usingRecursiveComparison().isEqualTo(new ReservationResponse(7L, 15L, "2099-09-14",
                "09:30", "CANCELLED", "2099-09-10T14:44:50Z", "HOLD_EXPIRED", "INVALID_PHONE_NUMBER",
                "Cambio solicitado por el usuario", "2099-09-10T14:30:00Z"));
    }

    @Test
    void writesTheInstantsInSeconds() {
        ReservationProcess process = ReservationProcess.start("user-42", 15L, LocalDate.of(2099, 9, 14),
                LocalTime.of(9, 30), Instant.parse("2099-09-10T14:30:00.243337795Z"));

        assertThat(reservationDtoMapper.toResponse(process).getCreatedAt()).isEqualTo("2099-09-10T14:30:00Z");
    }

    @Test
    void leavesTheDeadlineEmptyBeforeTheHoldExists() {
        ReservationProcess process = ReservationProcess.start("user-42", 15L, LocalDate.of(2099, 9, 14),
                LocalTime.of(9, 30), Instant.parse("2099-09-10T14:30:00Z"));

        assertThat(reservationDtoMapper.toResponse(process).getExpiresAt()).isNull();
    }
}
