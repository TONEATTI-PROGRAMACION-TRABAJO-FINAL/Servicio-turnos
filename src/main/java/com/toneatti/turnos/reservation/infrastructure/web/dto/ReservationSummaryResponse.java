package com.toneatti.turnos.reservation.infrastructure.web.dto;

import lombok.*;

/**
 * A row of the user's reservations as the cátedra has it (section 11 of the integration contract). Dates as
 * yyyy-MM-dd, times as HH:mm and instants as ISO-8601 in UTC. {@code reservationProcessId} is the id to cancel the
 * reservation with (section 12); the numeric id of the cátedra is not exposed.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationSummaryResponse {
    private String reservationProcessId;
    private Long professionalId;
    private String professionalFirstName;
    private String professionalLastName;
    private String date;
    private String startTime;
    private String endTime;
    private String status;
    private String createdAt;
    private String confirmedAt;
    private String cancelledAt;
    private String cancellationReason;
}
