package com.toneatti.turnos.reservation.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Local state of a reservation process. The states before a call with effects in the cátedra (PENDING_HOLD before
 * creating the hold, CONFIRMING before confirming it) are persisted first, so after a timeout the process records
 * that the effect may have happened and with which ids it can be reconciled (section 18.4 of the integration
 * contract).
 * <pre>
 * PENDING_HOLD ──▶ HELD ──▶ CONFIRMING ──▶ WAITING_FOR_PHONE
 *      │            │           │
 *      └──▶ FAILED ◀┴───────────┤
 *                   └──▶ EXPIRED ◀┘
 * </pre>
 */
public enum ReservationStatus {
    PENDING_HOLD,
    HELD,
    CONFIRMING,
    WAITING_FOR_PHONE,
    EXPIRED,
    FAILED;

    /**
     * EXPIRED and FAILED are final: a late answer or event cannot reopen them (section 18.3).
     */
    public boolean isFinal() {
        return this == EXPIRED || this == FAILED;
    }

    public boolean canTransitionTo(ReservationStatus next) {
        return nextStatuses().contains(next);
    }

    // PENDING_HOLD cannot expire: until the hold exists there is no expiresAt.
    private Set<ReservationStatus> nextStatuses() {
        return switch (this) {
            case PENDING_HOLD -> EnumSet.of(HELD, FAILED);
            case HELD -> EnumSet.of(CONFIRMING, EXPIRED, FAILED);
            case CONFIRMING -> EnumSet.of(WAITING_FOR_PHONE, EXPIRED, FAILED);
            // The phone step (Kafka) continues from here.
            case WAITING_FOR_PHONE, EXPIRED, FAILED -> EnumSet.noneOf(ReservationStatus.class);
        };
    }
}
