package com.toneatti.turnos.reservation.infrastructure.persistence.adapter;

import com.toneatti.turnos.reservation.domain.model.AppointmentActionOutcome;
import com.toneatti.turnos.reservation.domain.model.ProcessedAppointmentAction;
import com.toneatti.turnos.reservation.infrastructure.persistence.repository.JpaProcessedAppointmentActionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Against the H2 schema Hibernate generates, so the unique constraint on the eventId is the real one.
 */
@SpringBootTest
@ActiveProfiles("test")
class JpaProcessedAppointmentActionRepositoryAdapterIntegrationTest {

    @Autowired
    private JpaProcessedAppointmentActionRepositoryAdapter adapter;

    @Autowired
    private JpaProcessedAppointmentActionRepository jpaProcessedAppointmentActionRepository;

    @AfterEach
    void deleteActions() {
        jpaProcessedAppointmentActionRepository.deleteAll();
    }

    @Test
    void recordsAnEventWithIdAndFindsItByItsEventId() {
        ProcessedAppointmentAction created = adapter.create(processed("confirmation-event-7d10"));

        assertThat(created.id()).isNotNull();
        assertThat(created).usingRecursiveComparison().ignoringFields("id")
                .isEqualTo(processed("confirmation-event-7d10"));
        assertThat(adapter.existsByEventId("confirmation-event-7d10")).isTrue();
        assertThat(adapter.existsByEventId("rejection-event-7d11")).isFalse();
    }

    // Two consumers processing the same event at the same time: the second one fails and rolls back.
    @Test
    void rejectsTheSameEventIdTwice() {
        adapter.create(processed("confirmation-event-7d10"));

        assertThatThrownBy(() -> adapter.create(processed("confirmation-event-7d10")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jpaProcessedAppointmentActionRepository.count()).isEqualTo(1);
    }

    private static ProcessedAppointmentAction processed(String eventId) {
        return new ProcessedAppointmentAction(null, eventId, "AppointmentConfirmed", "process-a92b47d0",
                AppointmentActionOutcome.APPLIED, Instant.parse("2099-09-10T14:36:01Z"));
    }
}
