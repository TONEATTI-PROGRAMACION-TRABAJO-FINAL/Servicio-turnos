package com.toneatti.turnos.reservation.infrastructure.persistence.mapper;

import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import org.springframework.stereotype.Component;

@Component
public class ReservationProcessMapper {

    public ReservationProcessEntity toEntity(ReservationProcess process) {
        if (process == null) {
            return null;
        }
        return ReservationProcessEntity.builder()
                .id(process.getId())
                .userExternalId(process.getUserExternalId())
                .professionalId(process.getProfessionalId())
                .date(process.getDate())
                .startTime(process.getStartTime())
                .status(process.getStatus())
                .holdId(process.getHoldId())
                .reservationProcessId(process.getReservationProcessId())
                .expiresAt(process.getExpiresAt())
                .failureCode(process.getFailureCode())
                .requestEventId(process.getRequestEventId())
                .catedraReservationId(process.getCatedraReservationId())
                .phoneRejectionReason(process.getPhoneRejectionReason())
                .cancellationReason(process.getCancellationReason())
                // See the unique constraint of ReservationProcessEntity.
                .active(process.isActive() ? Boolean.TRUE : null)
                .createdAt(process.getCreatedAt())
                .updatedAt(process.getUpdatedAt())
                .version(process.getVersion())
                .build();
    }

    public ReservationProcess toDomainModel(ReservationProcessEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ReservationProcess(
                entity.getId(),
                entity.getUserExternalId(),
                entity.getProfessionalId(),
                entity.getDate(),
                entity.getStartTime(),
                entity.getStatus(),
                entity.getHoldId(),
                entity.getReservationProcessId(),
                entity.getExpiresAt(),
                entity.getFailureCode(),
                entity.getRequestEventId(),
                entity.getCatedraReservationId(),
                entity.getPhoneRejectionReason(),
                entity.getCancellationReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion());
    }
}
