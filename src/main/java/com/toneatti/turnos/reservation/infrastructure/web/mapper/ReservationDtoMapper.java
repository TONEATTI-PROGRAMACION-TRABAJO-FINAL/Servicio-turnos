package com.toneatti.turnos.reservation.infrastructure.web.mapper;

import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.infrastructure.web.dto.ReservationResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Component
public class ReservationDtoMapper {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    public ReservationResponse toResponse(ReservationProcess domain) {
        if (domain == null) {
            return null;
        }
        return ReservationResponse.builder()
                .id(domain.getId())
                .professionalId(domain.getProfessionalId())
                .date(domain.getDate().toString())
                .startTime(domain.getStartTime().format(TIME))
                .status(domain.getStatus().name())
                .expiresAt(format(domain.getExpiresAt()))
                .failureCode(domain.getFailureCode())
                .phoneRejectionReason(domain.getPhoneRejectionReason())
                .cancellationReason(domain.getCancellationReason())
                .createdAt(format(domain.getCreatedAt()))
                .build();
    }

    // In seconds, like the instants of the cátedra: the clock has nanoseconds and the database keeps microseconds,
    // so a process read back would otherwise show another createdAt.
    private static String format(Instant instant) {
        return instant == null ? null : instant.truncatedTo(ChronoUnit.SECONDS).toString();
    }
}
