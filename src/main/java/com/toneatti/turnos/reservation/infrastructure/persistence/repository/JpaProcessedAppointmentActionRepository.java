package com.toneatti.turnos.reservation.infrastructure.persistence.repository;

import com.toneatti.turnos.reservation.infrastructure.persistence.entity.ProcessedAppointmentActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaProcessedAppointmentActionRepository extends JpaRepository<ProcessedAppointmentActionEntity, Long> {

    boolean existsByEventId(String eventId);

}
