package com.toneatti.turnos.reservation.infrastructure.web.mapper;

import com.toneatti.turnos.catedra.domain.model.AppointmentPage;
import com.toneatti.turnos.catedra.domain.model.AppointmentSummary;
import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.infrastructure.web.dto.ReservationPageResponse;
import com.toneatti.turnos.reservation.infrastructure.web.dto.ReservationResponse;
import com.toneatti.turnos.reservation.infrastructure.web.dto.ReservationSummaryResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalTime;
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

    public ReservationSummaryResponse toSummaryResponse(AppointmentSummary domain) {
        if (domain == null) {
            return null;
        }
        return ReservationSummaryResponse.builder()
                .reservationProcessId(domain.reservationProcessId())
                .professionalId(domain.professionalId())
                .professionalFirstName(domain.professionalFirstName())
                .professionalLastName(domain.professionalLastName())
                .date(domain.date().toString())
                .startTime(domain.startTime().format(TIME))
                .endTime(format(domain.endTime()))
                .status(domain.status().name())
                .createdAt(format(domain.createdAt()))
                .confirmedAt(format(domain.confirmedAt()))
                .cancelledAt(format(domain.cancelledAt()))
                .cancellationReason(domain.cancellationReason())
                .build();
    }

    public ReservationPageResponse toPageResponse(AppointmentPage domain, int page, int size) {
        return ReservationPageResponse.builder()
                .content(domain.content().stream().map(this::toSummaryResponse).toList())
                .totalCount(domain.totalCount())
                .page(page)
                .size(size)
                .build();
    }

    private static String format(LocalTime time) {
        return time == null ? null : time.format(TIME);
    }

    // In seconds, like the instants of the cátedra: the clock has nanoseconds and the database keeps microseconds,
    // so a process read back would otherwise show another createdAt.
    private static String format(Instant instant) {
        return instant == null ? null : instant.truncatedTo(ChronoUnit.SECONDS).toString();
    }
}
