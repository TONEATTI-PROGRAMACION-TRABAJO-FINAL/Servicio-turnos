package com.toneatti.turnos.reservation.infrastructure.persistence.adapter;

import com.toneatti.turnos.reservation.application.exception.ConcurrentReservationUpdateException;
import com.toneatti.turnos.reservation.application.exception.ReservationInProgressException;
import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.model.ReservationStatus;
import com.toneatti.turnos.reservation.infrastructure.persistence.repository.JpaReservationProcessRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Against the H2 schema Hibernate generates, so the unique constraint and the optimistic lock are the real ones.
 */
@SpringBootTest
@ActiveProfiles("test")
class JpaReservationProcessRepositoryAdapterIntegrationTest {

    private static final LocalDate DAY = LocalDate.of(2099, 9, 14);
    private static final LocalTime NINE_THIRTY = LocalTime.of(9, 30);
    private static final Instant NOW = Instant.parse("2099-09-10T14:30:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2099-09-10T14:44:50Z");

    @Autowired
    private JpaReservationProcessRepositoryAdapter adapter;

    @Autowired
    private JpaReservationProcessRepository jpaReservationProcessRepository;

    @AfterEach
    void deleteProcesses() {
        jpaReservationProcessRepository.deleteAll();
    }

    @Test
    void createsAProcessWithIdAndVersion() {
        ReservationProcess created = adapter.create(started("user-42"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getVersion()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(ReservationStatus.PENDING_HOLD);
    }

    @Test
    void findsTheActiveProcessOfTheUserForTheSlot() {
        ReservationProcess created = adapter.create(started("user-42"));

        assertThat(adapter.findActiveByUserAndSlot("user-42", 15L, DAY, NINE_THIRTY))
                .hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(created.getId()));
        assertThat(adapter.findActiveByUserAndSlot("user-43", 15L, DAY, NINE_THIRTY)).isEmpty();
        assertThat(adapter.findActiveByUserAndSlot("user-42", 15L, DAY, NINE_THIRTY.plusMinutes(30))).isEmpty();
    }

    @Test
    void findsAProcessByIdOnlyForItsOwner() {
        ReservationProcess created = adapter.create(started("user-42"));

        assertThat(adapter.findByIdAndUserExternalId(created.getId(), "user-42"))
                .hasValueSatisfying(found -> assertThat(found.getUserExternalId()).isEqualTo("user-42"));
        assertThat(adapter.findByIdAndUserExternalId(created.getId(), "user-43")).isEmpty();
        assertThat(adapter.findByIdAndUserExternalId(created.getId() + 1, "user-42")).isEmpty();
    }

    @Test
    void findsAProcessByTheReservationProcessIdOfTheCatedra() {
        ReservationProcess process = adapter.create(started("user-42"));
        process.markHeld("hold-6c0c2f41", "process-a92b47d0", EXPIRES_AT, NOW);
        adapter.update(process.getId(), process);

        assertThat(adapter.findByReservationProcessId("process-a92b47d0"))
                .hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(process.getId()));
        assertThat(adapter.findByReservationProcessId("process-other")).isEmpty();
    }

    @Test
    void findsAProcessByTheReservationProcessIdOfTheCatedraOnlyForItsOwner() {
        ReservationProcess process = adapter.create(started("user-42"));
        process.markHeld("hold-6c0c2f41", "process-a92b47d0", EXPIRES_AT, NOW);
        adapter.update(process.getId(), process);

        assertThat(adapter.findByReservationProcessIdAndUserExternalId("process-a92b47d0", "user-42"))
                .hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(process.getId()));
        assertThat(adapter.findByReservationProcessIdAndUserExternalId("process-a92b47d0", "user-43")).isEmpty();
        assertThat(adapter.findByReservationProcessIdAndUserExternalId("process-other", "user-42")).isEmpty();
    }

    @Test
    void rejectsASecondActiveProcessOfTheUserForTheSameSlot() {
        adapter.create(started("user-42"));

        assertThatThrownBy(() -> adapter.create(started("user-42")))
                .isInstanceOf(ReservationInProgressException.class);
    }

    @Test
    void allowsTheSameSlotToOtherUsers() {
        adapter.create(started("user-42"));

        assertThat(adapter.create(started("user-43")).getId()).isNotNull();
    }

    @Test
    void allowsANewProcessOnceTheOthersOfTheSlotFinished() {
        finish(adapter.create(started("user-42")));
        finish(adapter.create(started("user-42")));

        assertThat(adapter.create(started("user-42")).getId()).isNotNull();
        assertThat(adapter.findActiveByUserAndSlot("user-42", 15L, DAY, NINE_THIRTY)).isPresent();
    }

    @Test
    void rethrowsAnIntegrityViolationNotCausedByTheActiveSlot() {
        ReservationProcess first = adapter.create(started("user-42"));
        first.markHeld("hold-6c0c2f41", "process-a92b47d0", EXPIRES_AT, NOW);
        adapter.update(first.getId(), first);
        ReservationProcess second = started("user-43");
        second.markHeld("hold-6c0c2f41", "process-other", EXPIRES_AT, NOW);

        assertThatThrownBy(() -> adapter.create(second)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void updatesTheStatusAndTheIdsOfTheCatedra() {
        ReservationProcess process = adapter.create(started("user-42"));
        process.markHeld("hold-6c0c2f41", "process-a92b47d0", EXPIRES_AT, NOW.plusSeconds(1));

        ReservationProcess updated = adapter.update(process.getId(), process).orElseThrow();

        assertThat(updated.getStatus()).isEqualTo(ReservationStatus.HELD);
        assertThat(updated.getHoldId()).isEqualTo("hold-6c0c2f41");
        assertThat(updated.getReservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(updated.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(updated.getVersion()).isGreaterThan(process.getVersion());
    }

    @Test
    void doesNotUpdateAProcessThatDoesNotExist() {
        assertThat(adapter.update(999L, started("user-42"))).isEmpty();
    }

    // Two requests read the same process; the one that saves second must not overwrite the first.
    @Test
    void rejectsAnUpdateOverANewerVersion() {
        ReservationProcess created = adapter.create(started("user-42"));
        ReservationProcess first = adapter.findActiveByUserAndSlot("user-42", 15L, DAY, NINE_THIRTY).orElseThrow();
        ReservationProcess second = adapter.findActiveByUserAndSlot("user-42", 15L, DAY, NINE_THIRTY).orElseThrow();
        first.markHeld("hold-6c0c2f41", "process-a92b47d0", EXPIRES_AT, NOW);
        adapter.update(created.getId(), first);
        second.markFailed("SLOT_ALREADY_HELD", NOW);

        assertThatThrownBy(() -> adapter.update(created.getId(), second))
                .isInstanceOf(ConcurrentReservationUpdateException.class);
        assertThat(jpaReservationProcessRepository.findById(created.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.HELD);
    }

    private void finish(ReservationProcess process) {
        process.markFailed("SLOT_ALREADY_HELD", NOW);
        adapter.update(process.getId(), process);
    }

    private static ReservationProcess started(String userExternalId) {
        return ReservationProcess.start(userExternalId, 15L, DAY, NINE_THIRTY, NOW);
    }
}
