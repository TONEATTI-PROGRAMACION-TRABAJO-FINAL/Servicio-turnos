package com.toneatti.turnos.reservation.domain.model;

import com.toneatti.turnos.reservation.domain.model.AppointmentAction.AdditionalInformationRejected;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction.AdditionalInformationRequested;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction.AppointmentCancelled;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction.AppointmentConfirmed;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction.AppointmentProcessExpired;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction.AppointmentProcessInvalid;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A reservation that an end user of the app started for a slot of a professional. {@code userExternalId} is the
 * owner and the {@code externalPatientId} sent to the cátedra. {@code holdId}, {@code reservationProcessId} and
 * {@code expiresAt} come from the cátedra once the hold exists; {@code expiresAt} is the only source of the deadline.
 * The status only changes through the transition methods, which reject what {@link ReservationStatus} does not allow.
 * {@code version} detects two requests updating the same process at the same time.
 * From WAITING_FOR_PHONE on, the events of the cátedra drive it through {@link #apply}: {@code requestEventId} is the
 * event that asked for the phone, {@code catedraReservationId} the reservation of the cátedra once confirmed, and
 * {@code phoneRejectionReason} the reason of the last rejected phone.
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
    private String requestEventId;
    private Long catedraReservationId;
    private String phoneRejectionReason;
    private String cancellationReason;
    private final Instant createdAt;
    private Instant updatedAt;
    private final Long version;

    public ReservationProcess(Long id, String userExternalId, Long professionalId, LocalDate date,
                              LocalTime startTime, ReservationStatus status, String holdId,
                              String reservationProcessId, Instant expiresAt, String failureCode,
                              String requestEventId, Long catedraReservationId, String phoneRejectionReason,
                              String cancellationReason, Instant createdAt, Instant updatedAt, Long version) {
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
        this.requestEventId = requestEventId;
        this.catedraReservationId = catedraReservationId;
        this.phoneRejectionReason = phoneRejectionReason;
        this.cancellationReason = cancellationReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static ReservationProcess start(String userExternalId, Long professionalId, LocalDate date,
                                           LocalTime startTime, Instant now) {
        return new ReservationProcess(null, userExternalId, professionalId, date, startTime,
                ReservationStatus.PENDING_HOLD, null, null, null, null, null, null, null, null, now, now, null);
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
        updateExpiresAt(expiresAt);
    }

    public void markExpired(Instant now) {
        moveTo(ReservationStatus.EXPIRED, now);
    }

    public void markFailed(String failureCode, Instant now) {
        moveTo(ReservationStatus.FAILED, now);
        this.failureCode = failureCode;
    }

    /**
     * Applies an event of the cátedra and returns true, or returns false without changing anything when the current
     * status does not accept it: an event that arrives late, for example after the process expired, cannot reopen
     * or move back the process (section 18.3).
     */
    public boolean apply(AppointmentAction action, Instant now) {
        return switch (action) {
            case AdditionalInformationRequested requested -> requestPhone(requested, now);
            case AppointmentConfirmed confirmed -> confirm(confirmed, now);
            case AdditionalInformationRejected rejected -> rejectPhone(rejected, now);
            case AppointmentProcessExpired expired -> finish(ReservationStatus.EXPIRED, expired.reason(), now);
            case AppointmentProcessInvalid invalid -> finish(ReservationStatus.FAILED, invalid.reason(), now);
            case AppointmentCancelled cancelled -> cancel(cancelled, now);
        };
    }

    // The event can arrive before the answer of the confirmation is saved, while the process is still CONFIRMING.
    private boolean requestPhone(AdditionalInformationRequested requested, Instant now) {
        if (status == ReservationStatus.CONFIRMING) {
            moveTo(ReservationStatus.WAITING_FOR_PHONE, now);
        } else if (status == ReservationStatus.WAITING_FOR_PHONE) {
            this.updatedAt = now;
        } else {
            return false;
        }
        this.requestEventId = requested.eventId();
        updateExpiresAt(requested.expiresAt());
        return true;
    }

    private boolean confirm(AppointmentConfirmed confirmed, Instant now) {
        if (!status.canTransitionTo(ReservationStatus.CONFIRMED)) {
            return false;
        }
        moveTo(ReservationStatus.CONFIRMED, now);
        this.catedraReservationId = confirmed.reservationId();
        return true;
    }

    // Not a transition: the process keeps waiting for a valid phone.
    private boolean rejectPhone(AdditionalInformationRejected rejected, Instant now) {
        if (status != ReservationStatus.WAITING_FOR_PHONE) {
            return false;
        }
        this.phoneRejectionReason = rejected.reason();
        updateExpiresAt(rejected.expiresAt());
        this.updatedAt = now;
        return true;
    }

    /**
     * The user sent a phone number. Not a transition: the process keeps waiting until the cátedra answers, and the
     * rejection of the previous phone no longer applies, so the app does not take it as the answer to this one.
     */
    public void submitPhone(Instant now) {
        if (status != ReservationStatus.WAITING_FOR_PHONE) {
            throw new IllegalStateException("Reservation process " + id + " is not waiting for a phone but " + status);
        }
        this.phoneRejectionReason = null;
        this.updatedAt = now;
    }

    private boolean finish(ReservationStatus next, String reason, Instant now) {
        if (!status.canTransitionTo(next)) {
            return false;
        }
        moveTo(next, now);
        this.failureCode = reason;
        return true;
    }

    private boolean cancel(AppointmentCancelled cancelled, Instant now) {
        if (!status.canTransitionTo(ReservationStatus.CANCELLED)) {
            return false;
        }
        moveTo(ReservationStatus.CANCELLED, now);
        this.cancellationReason = cancelled.reason();
        return true;
    }

    private void updateExpiresAt(Instant expiresAt) {
        if (expiresAt != null) {
            this.expiresAt = expiresAt;
        }
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

    public String getRequestEventId() {
        return requestEventId;
    }

    public Long getCatedraReservationId() {
        return catedraReservationId;
    }

    public String getPhoneRejectionReason() {
        return phoneRejectionReason;
    }

    public String getCancellationReason() {
        return cancellationReason;
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
