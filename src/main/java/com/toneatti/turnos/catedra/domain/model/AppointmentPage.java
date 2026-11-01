package com.toneatti.turnos.catedra.domain.model;

import java.util.List;

/**
 * A page of the reservations of a patient (section 11), with the total count the cátedra reports independently of
 * the page.
 */
public record AppointmentPage(
        List<AppointmentSummary> content,
        long totalCount
) {
}
