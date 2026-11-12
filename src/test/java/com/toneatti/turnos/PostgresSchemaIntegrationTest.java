package com.toneatti.turnos;

import com.toneatti.turnos.reservation.application.exception.ReservationInProgressException;
import com.toneatti.turnos.reservation.infrastructure.persistence.adapter.JpaReservationProcessRepositoryAdapter;
import com.toneatti.turnos.reservation.infrastructure.persistence.repository.JpaReservationProcessRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static com.toneatti.turnos.reservation.infrastructure.persistence.adapter.ReservationProcessAdapterFixtures.finish;
import static com.toneatti.turnos.reservation.infrastructure.persistence.adapter.ReservationProcessAdapterFixtures.started;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The Flyway migrations against a real PostgreSQL, the same image as compose.yaml: the context only starts when they
 * apply and Hibernate validates the schema they create. The other tests run them on H2 in PostgreSQL mode.
 * Skipped where Docker is not available.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PostgresSchemaIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:18"));

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private Flyway flyway;

    @Autowired
    private JpaReservationProcessRepositoryAdapter adapter;

    @Autowired
    private JpaReservationProcessRepository jpaReservationProcessRepository;

    @AfterEach
    void deleteProcesses() {
        jpaReservationProcessRepository.deleteAll();
    }

    @Test
    void appliesTheMigrationsThatHibernateValidates() {
        MigrationInfo current = flyway.info().current();

        assertThat(current.getVersion().getVersion()).isEqualTo("1");
        assertThat(current.getState()).isEqualTo(MigrationState.SUCCESS);
    }

    // active is NULL once a process is final, and PostgreSQL treats NULLs as distinct in a unique constraint.
    @Test
    void allowsManyFinishedProcessesForTheSlotButOnlyOneActive() {
        finish(adapter, adapter.create(started("user-42")));
        finish(adapter, adapter.create(started("user-42")));

        assertThat(adapter.create(started("user-42")).getId()).isNotNull();
        assertThatThrownBy(() -> adapter.create(started("user-42")))
                .isInstanceOf(ReservationInProgressException.class);
    }
}
