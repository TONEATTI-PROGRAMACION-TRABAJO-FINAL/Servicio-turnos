package com.toneatti.turnos.availability.infrastructure.web.dto;

import lombok.*;

/**
 * Dates as yyyy-MM-dd and times as HH:mm, the format the cátedra accepts when creating a hold.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableSlotResponse {
    private String date;
    private String startTime;
    private String endTime;
}
