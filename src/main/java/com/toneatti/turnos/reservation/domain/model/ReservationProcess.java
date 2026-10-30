package com.toneatti.turnos.reservation.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A reservation that an end user of the app started for a slot of a professional. {@code userExternalId} is the
 * owner and the {@code externalPatientId} sent to the cátedra. {@code holdId}, {@code reservationProcessId} and
 * {@code expiresAt} come from the cátedra once the hold exists; {@code expiresAt} is the only source of the deadline.
 * The status only changes through the transition methods, which reject what {@link ReservationStatus} does not allow.
 * {@code version} detects two requests updating the same process at the same time.
 */
public class ReservationProcess {

    private final Long id;
    private final String userExternalId;
    private final Long professionalId;
    private final LocalDate date;
    private final LocalTime startTime;
    private ReservationStatus status;
    private String holdId;
    private String reservationProcessId;
    private Instant expiresAt;
    private String failureCode;
    private final Instant createdAt;
    private Instant updatedAt;
    private final Long version;

    public ReservationProcess(Long id, String userExternalId, Long professionalId, LocalDate date,
                              LocalTime startTime, ReservationStatus status, String holdId,
                              String reservationProcessId, Instant expiresAt, String failureCode,
                              Instant createdAt, Instant updatedAt, Long version) {
        this.id = id;
        this.userExternalId = userExternalId;
        this.professionalId = professionalId;
        this.date = date;
        this.startTime = startTime;
        this.status = status;
        this.holdId = holdId;
        this.reservationProcessId = reservationProcessId;
        this.expiresAt = expiresAt;
        this.failureCode = failureCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static ReservationProcess start(String userExternalId, Long professionalId, LocalDate date,
                                           LocalTime startTime, Instant now) {
        return new ReservationProcess(null, userExternalId, professionalId, date, startTime,
                ReservationStatus.PENDING_HOLD, null, null, null, null, now, now, null);
    }

    public void markHeld(String holdId, String reservationProcessId, Instant expiresAt, Instant now) {
        moveTo(ReservationStatus.HELD, now);
        this.holdId = holdId;
        this.reservationProcessId = reservationProcessId;
        this.expiresAt = expiresAt;
    }

    public void markConfirming(Instant now) {
        moveTo(ReservationStatus.CONFIRMING, now);
    }

    // The cátedra answers the confirmation with the deadline of the whole process, which replaces the one of the hold.
    public void markWaitingForPhone(Instant expiresAt, Instant now) {
        moveTo(ReservationStatus.WAITING_FOR_PHONE, now);
        if (expiresAt != null) {
            this.expiresAt = expiresAt;
        }
    }

    public void markExpired(Instant now) {
        moveTo(ReservationStatus.EXPIRED, now);
    }

    public void markFailed(String failureCode, Instant now) {
        moveTo(ReservationStatus.FAILED, now);
        this.failureCode = failureCode;
    }

    /**
     * True once the deadline of the cátedra passed; a process without a hold yet has no deadline.
     */
    public boolean isExpiredAt(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }

    public boolean isActive() {
        return !status.isFinal();
    }

    private void moveTo(ReservationStatus next, Instant now) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException("Reservation process " + id + " cannot go from " + status + " to " + next);
        }
        this.status = next;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getUserExternalId() {
        return userExternalId;
    }

    public Long getProfessionalId() {
        return professionalId;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public String getHoldId() {
        return holdId;
    }

    public String getReservationProcessId() {
        return reservationProcessId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
