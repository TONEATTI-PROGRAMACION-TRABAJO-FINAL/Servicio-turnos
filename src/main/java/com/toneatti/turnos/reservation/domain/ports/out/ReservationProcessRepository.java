package com.toneatti.turnos.reservation.domain.ports.out;

import com.toneatti.turnos.reservation.domain.model.ReservationProcess;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public interface ReservationProcessRepository {

    ReservationProcess create(ReservationProcess process);
    Optional<ReservationProcess> update(Long id, ReservationProcess process);
    Optional<ReservationProcess> findByIdAndUserExternalId(Long id, String userExternalId);
    Optional<ReservationProcess> findByReservationProcessId(String reservationProcessId);
    Optional<ReservationProcess> findActiveByUserAndSlot(String userExternalId, Long professionalId,
                                                         LocalDate date, LocalTime startTime);

}
