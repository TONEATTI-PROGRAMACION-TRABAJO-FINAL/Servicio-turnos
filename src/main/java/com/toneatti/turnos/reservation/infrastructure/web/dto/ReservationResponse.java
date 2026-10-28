package com.toneatti.turnos.reservation.infrastructure.web.dto;

import lombok.*;

/**
 * Dates as yyyy-MM-dd, times as HH:mm and instants as ISO-8601 in UTC. The ids of the cátedra are not exposed.
 * {@code failureCode} is why the process ended FAILED or EXPIRED, {@code phoneRejectionReason} why the last phone was
 * rejected and {@code cancellationReason} why the reservation was cancelled; each is null when it does not apply.
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
    private String failureCode;
    private String phoneRejectionReason;
    private String cancellationReason;
    private String createdAt;
}
