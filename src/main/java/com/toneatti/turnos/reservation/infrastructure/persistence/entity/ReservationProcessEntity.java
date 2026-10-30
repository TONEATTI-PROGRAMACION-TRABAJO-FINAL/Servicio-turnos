package com.toneatti.turnos.reservation.infrastructure.persistence.entity;

import com.toneatti.turnos.reservation.domain.model.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

// The unique constraint also serves the searches by user, since user_external_id is its first column.
@Getter
@Setter
@Entity
@Table(name = "reservation_processes", uniqueConstraints = @UniqueConstraint(
        name = "uk_reservation_processes_active_slot",
        columnNames = {"user_external_id", "professional_id", "slot_date", "start_time", "active"}))
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationProcessEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, updatable = false, length = 100)
    private String userExternalId;
    @Column(nullable = false, updatable = false)
    private Long professionalId;
    // "date" is a reserved word in SQL.
    @Column(name = "slot_date", nullable = false, updatable = false)
    private LocalDate date;
    @Column(nullable = false, updatable = false)
    private LocalTime startTime;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;
    @Column(unique = true)
    private String holdId;
    @Column(unique = true)
    private String reservationProcessId;
    private Instant expiresAt;
    @Column(length = 100)
    private String failureCode;
    private String requestEventId;
    private Long catedraReservationId;
    @Column(length = 100)
    private String phoneRejectionReason;
    // Up to 500 characters in the cátedra (section 12).
    @Column(length = 500)
    private String cancellationReason;
    // TRUE while the process is active and NULL once it is final, never FALSE: a unique constraint treats NULLs as
    // distinct, so it allows any number of finished processes for the same user and slot but only one active one.
    // H2 does not support the partial unique index (WHERE active) that would say the same thing directly.
    private Boolean active;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    @Version
    private Long version;
}
