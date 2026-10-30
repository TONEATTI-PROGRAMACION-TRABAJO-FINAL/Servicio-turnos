package com.toneatti.turnos.reservation.infrastructure.kafka.listener;

import com.toneatti.turnos.reservation.application.service.ReservationService;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction.AdditionalInformationRejected;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction.AdditionalInformationRequested;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction.AppointmentConfirmed;
import com.toneatti.turnos.reservation.domain.model.AppointmentActionOutcome;
import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.model.ReservationStatus;
import com.toneatti.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import com.toneatti.turnos.reservation.infrastructure.persistence.entity.ProcessedAppointmentActionEntity;
import com.toneatti.turnos.reservation.infrastructure.persistence.repository.JpaProcessedAppointmentActionRepository;
import com.toneatti.turnos.reservation.infrastructure.persistence.repository.JpaReservationProcessRepository;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.CONFIRMED;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.CONFIRMING;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.EXPIRED;
import static com.toneatti.turnos.reservation.domain.model.ReservationStatus.WAITING_FOR_PHONE;
import static com.toneatti.turnos.reservation.infrastructure.kafka.AppointmentActionMessages.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * The consumer against an embedded broker, never the environment of the cátedra. Each test uses its own
 * reservationProcessId, which is also the message key, so the events of a test keep their order and the other tests
 * do not touch its process.
 */
@SpringBootTest(properties = {
        "catedra.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "catedra.kafka.listener-enabled=true",
        // One retry after a second, then the event is skipped.
        "catedra.kafka.retry-max-elapsed-time=PT1S"})
@EmbeddedKafka(partitions = 1, topics = AppointmentActionListenerIntegrationTest.TOPIC)
@ActiveProfiles("test")
class AppointmentActionListenerIntegrationTest {

    // From application-test.properties.
    static final String TOPIC = "alumnos.turnos.acciones.test";
    private static final Instant NOW = Instant.parse("2099-09-10T14:30:00Z");
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private ReservationProcessRepository reservationProcessRepository;

    @Autowired
    private JpaProcessedAppointmentActionRepository jpaProcessedAppointmentActionRepository;

    @Autowired
    private JpaReservationProcessRepository jpaReservationProcessRepository;

    @MockitoSpyBean
    private ReservationService reservationService;

    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void createProducer() {
        kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                KafkaTestUtils.producerProps(broker), new StringSerializer(), new StringSerializer()));
    }

    // The in-memory database is shared with the other tests of the JVM.
    @AfterEach
    void cleanUp() {
        kafkaTemplate.destroy();
        jpaProcessedAppointmentActionRepository.deleteAll();
        jpaReservationProcessRepository.deleteAll();
    }

    @Test
    void followsTheProcessFromThePhoneRequestToTheConfirmation() {
        String processId = givenAProcessIn(CONFIRMING);

        publish(requested(processId));
        publish(new AppointmentConfirmed(newEventId(), processId, 8251L));

        awaitTheStatus(processId, CONFIRMED);
        ReservationProcess process = processWith(processId);
        assertThat(process.getRequestEventId()).startsWith("event-");
        assertThat(process.getCatedraReservationId()).isEqualTo(8251L);
    }

    // Section 18.1: the redelivered rejection is recorded once and the process continues.
    @Test
    void ignoresARedeliveredEvent() {
        String processId = givenAProcessIn(WAITING_FOR_PHONE);
        AdditionalInformationRejected rejection = new AdditionalInformationRejected(newEventId(), processId,
                "INVALID_PHONE_NUMBER", NOW.plusSeconds(900));

        AppointmentConfirmed confirmation = new AppointmentConfirmed(newEventId(), processId, 8251L);

        publish(rejection);
        publish(rejection);
        publish(confirmation);

        awaitTheStatus(processId, CONFIRMED);
        assertThat(jpaProcessedAppointmentActionRepository.findAll())
                .filteredOn(processed -> processed.getReservationProcessId().equals(processId))
                .extracting(ProcessedAppointmentActionEntity::getEventId)
                .containsExactlyInAnyOrder(rejection.eventId(), confirmation.eventId());
    }

    @Test
    void skipsUnreadableMessagesWithoutBlockingTheValidOnesBehindThem() {
        String processId = givenAProcessIn(CONFIRMING);

        publishRaw(processId, "not json");
        publishRaw(processId, null);
        publishRaw(processId, "{\"eventType\":\"AppointmentConfirmed\",\"reservationProcessId\":\"" + processId
                + "\"}");
        publishRaw(processId, json(requested(processId)).replace("AdditionalInformationRequested", "PhoneChanged"));
        publish(requested(processId));

        awaitTheStatus(processId, WAITING_FOR_PHONE);
        assertThat(processWith(processId).getRequestEventId()).isNotNull();
    }

    // Section 18.3: the event is recorded, so its redelivery is a duplicate, but the process stays expired.
    @Test
    void aLateEventDoesNotReopenAnExpiredProcess() {
        String processId = givenAProcessIn(EXPIRED);

        AppointmentConfirmed confirmation = new AppointmentConfirmed(newEventId(), processId, 8251L);
        publish(confirmation);

        awaitTheOutcome(confirmation, AppointmentActionOutcome.LATE);
        assertThat(processWith(processId).getStatus()).isEqualTo(EXPIRED);
    }

    @Test
    void recordsAnEventOfAProcessThatDoesNotExistLocally() {
        AppointmentConfirmed confirmation = new AppointmentConfirmed(newEventId(), "process-" + UUID.randomUUID(),
                8251L);

        publish(confirmation);

        awaitTheOutcome(confirmation, AppointmentActionOutcome.UNKNOWN_PROCESS);
    }

    @Test
    void retriesAnEventWhoseProcessingFailedOnce() {
        String processId = givenAProcessIn(CONFIRMING);
        AdditionalInformationRequested requested = requested(processId);
        doThrow(new IllegalStateException("database down")).doCallRealMethod()
                .when(reservationService).applyAppointmentAction(requested);

        publish(requested);

        awaitTheStatus(processId, WAITING_FOR_PHONE);
        verify(reservationService, atLeast(2)).applyAppointmentAction(requested);
    }

    // After the retries the failing event is logged and skipped, so the next one of the partition is processed.
    @Test
    void skipsAnEventThatKeepsFailingWithoutBlockingTheNextOne() {
        String processId = givenAProcessIn(CONFIRMING);
        AdditionalInformationRequested failing = requested(processId);
        doThrow(new IllegalStateException("database down")).when(reservationService).applyAppointmentAction(failing);

        publish(failing);
        publish(requested(processId));

        awaitTheStatus(processId, WAITING_FOR_PHONE);
        verify(reservationService, atLeast(2)).applyAppointmentAction(failing);
        assertThat(jpaProcessedAppointmentActionRepository.existsByEventId(failing.eventId())).isFalse();
    }

    private String givenAProcessIn(ReservationStatus status) {
        String processId = "process-" + UUID.randomUUID();
        ReservationProcess process = ReservationProcess.start("user-" + processId, 15L, LocalDate.of(2099, 9, 14),
                LocalTime.of(9, 30), NOW);
        process.markHeld("hold-" + processId, processId, NOW.plusSeconds(890), NOW);
        process.markConfirming(NOW);
        if (status == WAITING_FOR_PHONE) {
            process.markWaitingForPhone(null, NOW);
        } else if (status == EXPIRED) {
            process.markExpired(NOW);
        }
        reservationProcessRepository.create(process);
        return processId;
    }

    private static AdditionalInformationRequested requested(String processId) {
        return new AdditionalInformationRequested(newEventId(), processId, NOW.plusSeconds(900));
    }

    private static String newEventId() {
        return "event-" + UUID.randomUUID();
    }

    private void publish(AppointmentAction action) {
        publishRaw(action.reservationProcessId(), json(action));
    }

    // The message key is the reservationProcessId (section 15.1).
    private void publishRaw(String key, String value) {
        try {
            kafkaTemplate.send(TOPIC, key, value).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Could not publish to the embedded broker", e);
        }
    }

    private ReservationProcess processWith(String processId) {
        return reservationProcessRepository.findByReservationProcessId(processId).orElseThrow();
    }

    private void awaitTheStatus(String processId, ReservationStatus status) {
        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(processWith(processId).getStatus()).isEqualTo(status));
    }

    private void awaitTheOutcome(AppointmentAction action, AppointmentActionOutcome outcome) {
        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(jpaProcessedAppointmentActionRepository.findAll())
                .filteredOn(processed -> processed.getEventId().equals(action.eventId()))
                .extracting(ProcessedAppointmentActionEntity::getOutcome)
                .containsExactly(outcome));
    }
}
