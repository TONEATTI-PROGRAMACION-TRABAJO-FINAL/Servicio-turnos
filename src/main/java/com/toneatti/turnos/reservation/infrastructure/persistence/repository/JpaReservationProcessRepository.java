package com.toneatti.turnos.reservation.infrastructure.persistence.repository;

import com.toneatti.turnos.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@Repository
public interface JpaReservationProcessRepository extends JpaRepository<ReservationProcessEntity, Long> {

    Optional<ReservationProcessEntity> findByUserExternalIdAndProfessionalIdAndDateAndStartTimeAndActiveTrue(
            String userExternalId, Long professionalId, LocalDate date, LocalTime startTime);

}
