package com.toneatti.turnos.reservation.infrastructure.web.controller;

import com.toneatti.turnos.reservation.application.exception.PhoneSubmissionUnavailableException;
import com.toneatti.turnos.reservation.domain.model.ReservationStatus;
import com.toneatti.turnos.reservation.domain.ports.out.PhoneSubmissionPublisher;
import com.toneatti.turnos.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import com.toneatti.turnos.reservation.infrastructure.persistence.repository.JpaReservationProcessRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.toneatti.turnos.reservation.infrastructure.kafka.PhoneTopicRecords.recordsWithKey;
import static com.toneatti.turnos.reservation.infrastructure.kafka.config.CatedraKafkaTestProperties.PHONE_TOPIC;
import static com.toneatti.turnos.support.ProblemAssertions.expectProblem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /reservations/{id}/phone against an embedded broker, never the environment of the cátedra. Each test stores
 * its own process with its own reservationProcessId, which is also the message key, so it only reads its own events.
 */
@SpringBootTest(properties = "catedra.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = PHONE_TOPIC)
@ActiveProfiles("test")
class PhoneSubmissionIntegrationTest {

    private static final String TYPED_PHONE = """
            {"phoneNumber":"+54 9 (261) 555-1234"}
            """;
    private static final Instant FUTURE = Instant.parse("2099-09-10T14:44:50Z");
    private static final Instant PAST = Instant.parse("2000-01-01T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JpaReservationProcessRepository jpaReservationProcessRepository;

    @MockitoSpyBean
    private PhoneSubmissionPublisher phoneSubmissionPublisher;

    private String userExternalId;
    private String processId;

    @BeforeEach
    void identifyTheTest() {
        userExternalId = UUID.randomUUID().toString();
        processId = "process-" + UUID.randomUUID();
    }

    // The in-memory database is shared with the other tests of the JVM.
    @AfterEach
    void deleteData() {
        jpaReservationProcessRepository.deleteAll();
    }

    @Test
    void sendsTheNormalizedPhoneToTheCatedraForTheEventThatRequestedIt() throws Exception {
        Long id = givenAProcess(ReservationStatus.WAITING_FOR_PHONE, "request-event-8a31", FUTURE);

        submitPhone(id, userExternalId, TYPED_PHONE)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("WAITING_FOR_PHONE"));

        ConsumerRecord<String, String> published = recordsWithKey(broker, processId, 1).getFirst();
        Map<String, Object> event = jsonMapper.readValue(published.value(), new TypeReference<>() {
        });
        assertThat(event).containsAllEntriesOf(Map.of(
                "eventType", "AdditionalInformationSubmitted",
                "groupId", "proyecto-test",
                "reservationProcessId", processId,
                "holdId", "hold-" + processId,
                "requestEventId", "request-event-8a31",
                "information", Map.of("phoneNumber", "+5492615551234"),
                "schemaVersion", 1));
        assertThat(event.keySet()).containsExactlyInAnyOrder("eventId", "eventType", "groupId",
                "reservationProcessId", "holdId", "requestEventId", "information", "occurredAt", "schemaVersion");
        assertThat(UUID.fromString((String) event.get("eventId"))).isNotNull();
        assertThat(Instant.parse((String) event.get("occurredAt"))).isBefore(FUTURE);
    }

    // Section 15.7: after a rejection the app sends another phone, which is another event, and the rejection of the
    // previous one is no longer shown.
    @Test
    void sendsAgainAfterARejectionAsANewEvent() throws Exception {
        Long id = givenAProcess(ReservationStatus.WAITING_FOR_PHONE, "request-event-8a31", FUTURE);

        submitPhone(id, userExternalId, TYPED_PHONE).andExpect(status().isAccepted());
        rejectTheLastPhone(id);
        submitPhone(id, userExternalId, TYPED_PHONE)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.phoneRejectionReason").isEmpty());

        List<ConsumerRecord<String, String>> published = recordsWithKey(broker, processId, 2);
        assertThat(published).hasSize(2);
        assertThat(published).extracting(consumerRecord -> jsonMapper.readValue(consumerRecord.value(), Map.class)
                .get("eventId")).doesNotHaveDuplicates();
        assertThat(jpaReservationProcessRepository.findById(id)).hasValueSatisfying(process -> {
            assertThat(process.getStatus()).isEqualTo(ReservationStatus.WAITING_FOR_PHONE);
            assertThat(process.getPhoneRejectionReason()).isNull();
        });
    }

    @Test
    void answers404ForAReservationOfAnotherUser() throws Exception {
        Long id = givenAProcess(ReservationStatus.WAITING_FOR_PHONE, "request-event-8a31", FUTURE);

        expectProblem(submitPhone(id, UUID.randomUUID().toString(), TYPED_PHONE), 404, "RESERVATION_NOT_FOUND");
        assertNothingWasPublished();
    }

    @Test
    void answers404ForAReservationThatDoesNotExist() throws Exception {
        expectProblem(submitPhone(999_999L, userExternalId, TYPED_PHONE), 404, "RESERVATION_NOT_FOUND");
        assertNothingWasPublished();
    }

    @Test
    void answers400WhenThePhoneIsMissing() throws Exception {
        Long id = givenAProcess(ReservationStatus.WAITING_FOR_PHONE, "request-event-8a31", FUTURE);

        expectProblem(submitPhone(id, userExternalId, "{\"phoneNumber\":\"  \"}"), 400, "VALIDATION_ERROR")
                .andExpect(jsonPath("$.detail").value("phoneNumber: Phone number is mandatory"));
        assertNothingWasPublished();
    }

    @Test
    void answers400WhenThePhoneIsNotValidForTheCatedra() throws Exception {
        Long id = givenAProcess(ReservationStatus.WAITING_FOR_PHONE, "request-event-8a31", FUTURE);

        expectProblem(submitPhone(id, userExternalId, "{\"phoneNumber\":\"261-555\"}"), 400,
                "INVALID_PHONE_NUMBER");
        assertNothingWasPublished();
    }

    @Test
    void answers409WhenTheReservationIsNotWaitingForThePhone() throws Exception {
        Long id = givenAProcess(ReservationStatus.CONFIRMED, "request-event-8a31", FUTURE);

        expectProblem(submitPhone(id, userExternalId, TYPED_PHONE), 409, "RESERVATION_NOT_WAITING_FOR_PHONE");
        assertNothingWasPublished();
    }

    @Test
    void answers409WhenTheCatedraHasNotRequestedThePhoneYet() throws Exception {
        Long id = givenAProcess(ReservationStatus.WAITING_FOR_PHONE, null, FUTURE);

        expectProblem(submitPhone(id, userExternalId, TYPED_PHONE), 409, "PHONE_NOT_REQUESTED_YET");
        assertNothingWasPublished();
    }

    @Test
    void answers410WhenTheReservationExpired() throws Exception {
        Long id = givenAProcess(ReservationStatus.WAITING_FOR_PHONE, "request-event-8a31", PAST);

        expectProblem(submitPhone(id, userExternalId, TYPED_PHONE), 410, "RESERVATION_EXPIRED");
        assertNothingWasPublished();
    }

    @Test
    void answers503AndKeepsWaitingWhenThePhoneCannotBeSent() throws Exception {
        Long id = givenAProcess(ReservationStatus.WAITING_FOR_PHONE, "request-event-8a31", FUTURE);
        doThrow(new PhoneSubmissionUnavailableException("Kafka is down", null))
                .when(phoneSubmissionPublisher).publish(any());

        expectProblem(submitPhone(id, userExternalId, TYPED_PHONE), 503, "PHONE_SUBMISSION_UNAVAILABLE");
        assertThat(jpaReservationProcessRepository.findById(id))
                .hasValueSatisfying(process -> assertThat(process.getStatus())
                        .isEqualTo(ReservationStatus.WAITING_FOR_PHONE));
    }

    @Test
    void answers401WithoutAUserToken() throws Exception {
        mockMvc.perform(post("/reservations/1/phone").contentType(MediaType.APPLICATION_JSON).content(TYPED_PHONE))
                .andExpect(status().isUnauthorized());
        assertNothingWasPublished();
    }

    private Long givenAProcess(ReservationStatus status, String requestEventId, Instant expiresAt) {
        Instant created = Instant.parse("2000-01-01T09:50:00Z");
        return jpaReservationProcessRepository.save(ReservationProcessEntity.builder()
                .userExternalId(userExternalId).professionalId(15L)
                .date(LocalDate.of(2099, 9, 14)).startTime(LocalTime.of(9, 30))
                .status(status).holdId("hold-" + processId).reservationProcessId(processId)
                .requestEventId(requestEventId).expiresAt(expiresAt).active(true)
                .createdAt(created).updatedAt(created)
                .build()).getId();
    }

    // What the consumer of the events of the cátedra saves for an AdditionalInformationRejected.
    private void rejectTheLastPhone(Long id) {
        ReservationProcessEntity process = jpaReservationProcessRepository.findById(id).orElseThrow();
        process.setPhoneRejectionReason("INVALID_PHONE_NUMBER");
        jpaReservationProcessRepository.save(process);
    }

    private ResultActions submitPhone(Long id, String subject, String body) throws Exception {
        return mockMvc.perform(post("/reservations/" + id + "/phone")
                .with(jwt().jwt(token -> token.subject(subject)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private void assertNothingWasPublished() {
        verify(phoneSubmissionPublisher, never()).publish(any());
    }
}
