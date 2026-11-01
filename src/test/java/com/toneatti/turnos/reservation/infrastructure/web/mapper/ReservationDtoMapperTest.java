package com.toneatti.turnos.reservation.infrastructure.web.mapper;

import com.toneatti.turnos.catedra.domain.model.AppointmentPage;
import com.toneatti.turnos.catedra.domain.model.AppointmentStatus;
import com.toneatti.turnos.catedra.domain.model.AppointmentSummary;
import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.model.ReservationStatus;
import com.toneatti.turnos.reservation.infrastructure.web.dto.ReservationPageResponse;
import com.toneatti.turnos.reservation.infrastructure.web.dto.ReservationResponse;
import com.toneatti.turnos.reservation.infrastructure.web.dto.ReservationSummaryResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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

    @Test
    void returnsNullForNullSummary() {
        assertThat(reservationDtoMapper.toSummaryResponse(null)).isNull();
    }

    // Neither reservationId nor the patient's data are exposed; reservationProcessId is, since it cancels it (T10).
    @Test
    void mapsEveryFieldOfASummaryWithoutThePatient() {
        AppointmentSummary summary = new AppointmentSummary(8251L, "process-a92b47d0", "user-42", 15L, "Ana",
                "Gomez", LocalDate.of(2026, 9, 14), LocalTime.of(9, 30), LocalTime.of(10, 0), "Juan", "Perez",
                "+5492615551234", AppointmentStatus.CONFIRMED, Instant.parse("2026-09-10T14:35:00Z"),
                Instant.parse("2026-09-10T14:36:00Z"), null, null);

        ReservationSummaryResponse response = reservationDtoMapper.toSummaryResponse(summary);

        assertThat(response).usingRecursiveComparison().isEqualTo(new ReservationSummaryResponse(
                "process-a92b47d0", 15L, "Ana", "Gomez", "2026-09-14", "09:30", "10:00", "CONFIRMED",
                "2026-09-10T14:35:00Z", "2026-09-10T14:36:00Z", null, null));
    }

    @Test
    void mapsAPageWithItsPageAndSize() {
        AppointmentSummary summary = new AppointmentSummary(8251L, "process-a92b47d0", "user-42", 15L, "Ana",
                "Gomez", LocalDate.of(2026, 9, 14), LocalTime.of(9, 30), LocalTime.of(10, 0), "Juan", "Perez",
                "+5492615551234", AppointmentStatus.CONFIRMED, Instant.parse("2026-09-10T14:35:00Z"),
                Instant.parse("2026-09-10T14:36:00Z"), null, null);
        AppointmentPage page = new AppointmentPage(List.of(summary), 37);

        ReservationPageResponse response = reservationDtoMapper.toPageResponse(page, 2, 20);

        assertThat(response.getContent()).usingRecursiveFieldByFieldElementComparator()
                .containsExactly(reservationDtoMapper.toSummaryResponse(summary));
        assertThat(response.getTotalCount()).isEqualTo(37);
        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getSize()).isEqualTo(20);
    }
}
