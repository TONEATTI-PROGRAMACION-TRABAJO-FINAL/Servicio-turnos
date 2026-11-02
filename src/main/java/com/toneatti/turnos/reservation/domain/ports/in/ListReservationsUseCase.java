package com.toneatti.turnos.reservation.domain.ports.in;

import com.toneatti.turnos.catedra.domain.model.AppointmentPage;
import com.toneatti.turnos.catedra.domain.model.AppointmentStatus;

import java.time.LocalDate;

public interface ListReservationsUseCase {
    AppointmentPage listReservations(String userExternalId, LocalDate from, LocalDate to, AppointmentStatus status,
                                     int page, int size);
}
