package com.toneatti.turnos.catedra.infrastructure.rest.mapper;

import com.toneatti.turnos.catedra.domain.model.Appointment;
import com.toneatti.turnos.catedra.domain.model.AppointmentCancellation;
import com.toneatti.turnos.catedra.domain.model.AppointmentStatus;
import com.toneatti.turnos.catedra.domain.model.AppointmentSummary;
import com.toneatti.turnos.catedra.domain.model.Hold;
import com.toneatti.turnos.catedra.domain.model.HoldConfirmation;
import com.toneatti.turnos.catedra.domain.model.Occupancy;
import com.toneatti.turnos.catedra.domain.model.OccupancyStatus;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraAppointmentListResponse;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraAppointmentResponse;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraCancellationResponse;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraHoldConfirmationResponse;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraHoldResponse;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraOccupancyResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class CatedraRestMapperTest {

    private final CatedraRestMapper catedraRestMapper = new CatedraRestMapper();

    @Test
    void returnsNullForNullInput() {
        assertThat(catedraRestMapper.toDomainModel((CatedraOccupancyResponse) null)).isNull();
        assertThat(catedraRestMapper.toDomainModel((CatedraHoldResponse) null)).isNull();
        assertThat(catedraRestMapper.toDomainModel((CatedraHoldConfirmationResponse) null)).isNull();
        assertThat(catedraRestMapper.toDomainModel((CatedraAppointmentResponse) null)).isNull();
        assertThat(catedraRestMapper.toDomainModel((CatedraAppointmentListResponse) null)).isNull();
        assertThat(catedraRestMapper.toDomainModel((CatedraCancellationResponse) null)).isNull();
    }

    @Test
    void mapsEveryFieldOfAnOccupancy() {
        CatedraOccupancyResponse response = new CatedraOccupancyResponse(LocalDate.of(2026, 9, 14),
                LocalTime.of(9, 30), OccupancyStatus.HELD, Instant.parse("2026-09-10T14:44:50Z"));

        Occupancy occupancy = catedraRestMapper.toDomainModel(response);

        assertThat(occupancy).usingRecursiveComparison().isEqualTo(response);
    }

    @Test
    void mapsEveryFieldOfAHold() {
        CatedraHoldResponse response = new CatedraHoldResponse("hold-6c0c2f41", "process-a92b47d0",
                Instant.parse("2026-09-10T14:44:50Z"));

        Hold hold = catedraRestMapper.toDomainModel(response);

        assertThat(hold).usingRecursiveComparison().isEqualTo(response);
    }

    @Test
    void mapsEveryFieldOfAHoldConfirmation() {
        CatedraHoldConfirmationResponse response = new CatedraHoldConfirmationResponse("process-a92b47d0",
                Instant.parse("2026-09-10T14:44:50Z"));

        HoldConfirmation confirmation = catedraRestMapper.toDomainModel(response);

        assertThat(confirmation).usingRecursiveComparison().isEqualTo(response);
    }

    @Test
    void mapsEveryFieldOfAnAppointment() {
        CatedraAppointmentResponse response = new CatedraAppointmentResponse(8251L, "process-a92b47d0", "user-42",
                15L, LocalDate.of(2026, 9, 14), LocalTime.of(9, 30), AppointmentStatus.PHONE_PENDING);

        Appointment appointment = catedraRestMapper.toDomainModel(response);

        assertThat(appointment).isEqualTo(new Appointment(8251L, "process-a92b47d0", "user-42",
                15L, LocalDate.of(2026, 9, 14), LocalTime.of(9, 30), AppointmentStatus.PHONE_PENDING));
    }

    @Test
    void mapsEveryFieldOfAnAppointmentSummary() {
        CatedraAppointmentListResponse response = new CatedraAppointmentListResponse(8251L, "process-a92b47d0",
                "user-42", 15L, "Ana", "Gomez", LocalDate.of(2026, 9, 14), LocalTime.of(9, 30), LocalTime.of(10, 0),
                "Juan", "Perez", "+5492615551234", AppointmentStatus.CONFIRMED,
                Instant.parse("2026-09-10T14:35:00Z"), Instant.parse("2026-09-10T14:36:00Z"), null, null);

        AppointmentSummary summary = catedraRestMapper.toDomainModel(response);

        assertThat(summary).isEqualTo(new AppointmentSummary(8251L, "process-a92b47d0", "user-42", 15L, "Ana",
                "Gomez", LocalDate.of(2026, 9, 14), LocalTime.of(9, 30), LocalTime.of(10, 0), "Juan", "Perez",
                "+5492615551234", AppointmentStatus.CONFIRMED, Instant.parse("2026-09-10T14:35:00Z"),
                Instant.parse("2026-09-10T14:36:00Z"), null, null));
    }

    @Test
    void mapsEveryFieldOfACancellation() {
        CatedraCancellationResponse response = new CatedraCancellationResponse(8251L, "process-a92b47d0",
                AppointmentStatus.CANCELLED, Instant.parse("2026-09-11T15:10:00Z"));

        AppointmentCancellation cancellation = catedraRestMapper.toDomainModel(response);

        assertThat(cancellation).isEqualTo(new AppointmentCancellation(8251L, "process-a92b47d0",
                Instant.parse("2026-09-11T15:10:00Z")));
    }
}
