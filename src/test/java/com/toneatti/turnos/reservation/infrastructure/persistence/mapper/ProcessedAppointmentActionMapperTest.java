package com.toneatti.turnos.reservation.infrastructure.persistence.mapper;

import com.toneatti.turnos.reservation.domain.model.AppointmentActionOutcome;
import com.toneatti.turnos.reservation.domain.model.ProcessedAppointmentAction;
import com.toneatti.turnos.reservation.infrastructure.persistence.entity.ProcessedAppointmentActionEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessedAppointmentActionMapperTest {

    private final ProcessedAppointmentActionMapper processedAppointmentActionMapper =
            new ProcessedAppointmentActionMapper();

    @Test
    void returnsNullForNullInput() {
        assertThat(processedAppointmentActionMapper.toEntity(null)).isNull();
        assertThat(processedAppointmentActionMapper.toDomainModel(null)).isNull();
    }

    @Test
    void mapsEveryFieldBothWays() {
        ProcessedAppointmentAction action = new ProcessedAppointmentAction(4L, "confirmation-event-7d10",
                "AppointmentConfirmed", "process-a92b47d0", AppointmentActionOutcome.APPLIED,
                Instant.parse("2099-09-10T14:36:01Z"));

        ProcessedAppointmentActionEntity entity = processedAppointmentActionMapper.toEntity(action);

        assertThat(entity).usingRecursiveComparison().isEqualTo(action);
        assertThat(processedAppointmentActionMapper.toDomainModel(entity)).isEqualTo(action);
    }
}
