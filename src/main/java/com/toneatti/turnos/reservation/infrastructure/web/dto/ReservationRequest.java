package com.toneatti.turnos.reservation.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Only the slot: the patient is the authenticated user, so its id and names are not taken from the app.
 * Dates as yyyy-MM-dd and times as HH:mm or HH:mm:ss.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {

    @NotNull(message = "Professional is mandatory")
    @Positive(message = "Professional must be a positive id")
    private Long professionalId;

    @NotNull(message = "Date is mandatory")
    private LocalDate date;

    @NotNull(message = "Start time is mandatory")
    private LocalTime startTime;
}
