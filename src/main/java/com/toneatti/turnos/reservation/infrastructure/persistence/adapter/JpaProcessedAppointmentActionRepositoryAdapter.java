package com.toneatti.turnos.reservation.infrastructure.persistence.adapter;

import com.toneatti.turnos.reservation.domain.model.ProcessedAppointmentAction;
import com.toneatti.turnos.reservation.domain.ports.out.ProcessedAppointmentActionRepository;
import com.toneatti.turnos.reservation.infrastructure.persistence.entity.ProcessedAppointmentActionEntity;
import com.toneatti.turnos.reservation.infrastructure.persistence.mapper.ProcessedAppointmentActionMapper;
import com.toneatti.turnos.reservation.infrastructure.persistence.repository.JpaProcessedAppointmentActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class JpaProcessedAppointmentActionRepositoryAdapter implements ProcessedAppointmentActionRepository {

    private final JpaProcessedAppointmentActionRepository jpaProcessedAppointmentActionRepository;
    private final ProcessedAppointmentActionMapper processedAppointmentActionMapper;

    // Flushed right away, so an eventId recorded meanwhile by another consumer fails here, inside the transaction of
    // the use case, with a DataIntegrityViolationException that rolls the whole event back.
    @Override
    public ProcessedAppointmentAction create(ProcessedAppointmentAction action) {
        ProcessedAppointmentActionEntity savedEntity = Objects.requireNonNull(
                jpaProcessedAppointmentActionRepository.saveAndFlush(processedAppointmentActionMapper.toEntity(action)),
                "saveAndFlush returned null");
        return processedAppointmentActionMapper.toDomainModel(savedEntity);
    }

    @Override
    public boolean existsByEventId(String eventId) {
        return jpaProcessedAppointmentActionRepository.existsByEventId(eventId);
    }
}
