package com.toneatti.turnos.reservation.infrastructure.persistence.adapter;

import com.toneatti.turnos.reservation.domain.model.ReservationProcess;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * The slot and the processes shared by the tests that save reservation processes through the adapter, on H2 and on
 * PostgreSQL.
 */
public final class ReservationProcessAdapterFixtures {

    public static final LocalDate DAY = LocalDate.of(2099, 9, 14);
    public static final LocalTime NINE_THIRTY = LocalTime.of(9, 30);
    public static final Instant NOW = Instant.parse("2099-09-10T14:30:00Z");

    private ReservationProcessAdapterFixtures() {
    }

    // A new process of the user for the slot of professional 15 on DAY at NINE_THIRTY.
    public static ReservationProcess started(String userExternalId) {
        return ReservationProcess.start(userExternalId, 15L, DAY, NINE_THIRTY, NOW);
    }

    // Leaves the process in a final status, so it no longer holds the slot.
    public static void finish(JpaReservationProcessRepositoryAdapter adapter, ReservationProcess process) {
        process.markFailed("SLOT_ALREADY_HELD", NOW);
        adapter.update(process.getId(), process);
    }
}
