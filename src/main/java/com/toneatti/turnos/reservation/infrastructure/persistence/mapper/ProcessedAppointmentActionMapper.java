package com.toneatti.turnos.reservation.infrastructure.persistence.mapper;

import com.toneatti.turnos.reservation.domain.model.ProcessedAppointmentAction;
import com.toneatti.turnos.reservation.infrastructure.persistence.entity.ProcessedAppointmentActionEntity;
import org.springframework.stereotype.Component;

@Component
public class ProcessedAppointmentActionMapper {

    public ProcessedAppointmentActionEntity toEntity(ProcessedAppointmentAction action) {
        if (action == null) {
            return null;
        }
        return ProcessedAppointmentActionEntity.builder()
                .id(action.id())
                .eventId(action.eventId())
                .eventType(action.eventType())
                .reservationProcessId(action.reservationProcessId())
                .outcome(action.outcome())
                .processedAt(action.processedAt())
                .build();
    }

    public ProcessedAppointmentAction toDomainModel(ProcessedAppointmentActionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ProcessedAppointmentAction(
                entity.getId(),
                entity.getEventId(),
                entity.getEventType(),
                entity.getReservationProcessId(),
                entity.getOutcome(),
                entity.getProcessedAt());
    }
}
