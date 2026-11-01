package com.toneatti.turnos.reservation.infrastructure.web.dto;

import lombok.*;

import java.util.List;

/**
 * {@code totalCount} is independent of the page (section 11); {@code page} and {@code size} echo what was
 * requested, so the app can ask for the next one.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationPageResponse {
    private List<ReservationSummaryResponse> content;
    private long totalCount;
    private int page;
    private int size;
}
