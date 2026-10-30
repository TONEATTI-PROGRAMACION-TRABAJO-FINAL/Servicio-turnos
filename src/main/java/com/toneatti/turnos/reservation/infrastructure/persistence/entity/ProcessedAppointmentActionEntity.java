package com.toneatti.turnos.reservation.infrastructure.persistence.entity;

import com.toneatti.turnos.reservation.domain.model.AppointmentActionOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

import java.time.Instant;

// The unique eventId is what guarantees that an event of the cátedra is processed once.
@Getter
@Setter
@Entity
@Table(name = "processed_appointment_actions", uniqueConstraints = @UniqueConstraint(
        name = "uk_processed_appointment_actions_event_id", columnNames = "event_id"))
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedAppointmentActionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, updatable = false)
    private String eventId;
    @Column(nullable = false, updatable = false, length = 50)
    private String eventType;
    @Column(nullable = false, updatable = false)
    private String reservationProcessId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private AppointmentActionOutcome outcome;
    @Column(nullable = false, updatable = false)
    private Instant processedAt;
}
