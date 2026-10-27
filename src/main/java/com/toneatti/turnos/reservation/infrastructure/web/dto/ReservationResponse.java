package com.toneatti.turnos.reservation.infrastructure.web.dto;

import lombok.*;

/**
 * Dates as yyyy-MM-dd, times as HH:mm and instants as ISO-8601 in UTC. The ids of the cátedra are not exposed.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    private Long id;
    private Long professionalId;
    private String date;
    private String startTime;
    private String status;
    private String expiresAt;
    private String createdAt;
}
