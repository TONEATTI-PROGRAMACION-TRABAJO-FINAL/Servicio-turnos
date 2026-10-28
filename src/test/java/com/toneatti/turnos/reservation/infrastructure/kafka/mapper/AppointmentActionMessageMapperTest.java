package com.toneatti.turnos.reservation.infrastructure.kafka.mapper;

import com.toneatti.turnos.reservation.domain.model.AppointmentAction;
import com.toneatti.turnos.reservation.infrastructure.kafka.dto.AppointmentActionMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.stream.Stream;

import static com.toneatti.turnos.reservation.domain.model.AppointmentActionFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentActionMessageMapperTest {

    private final AppointmentActionMessageMapper appointmentActionMessageMapper = new AppointmentActionMessageMapper();

    @Test
    void returnsNullForNullInput() {
        assertThat(appointmentActionMessageMapper.toDomain(null)).isNull();
    }

    @ParameterizedTest
    @MethodSource("eachEventOfTheContract")
    void mapsEachEventOfTheContract(AppointmentActionMessage message, AppointmentAction expected) {
        assertThat(appointmentActionMessageMapper.toDomain(message)).isEqualTo(expected);
    }

    static Stream<Arguments> eachEventOfTheContract() {
        return Stream.of(
                Arguments.of(message(REQUESTED, null, null, REQUESTED.expiresAt()), REQUESTED),
                Arguments.of(message(CONFIRMATION, 8251L, null, null), CONFIRMATION),
                Arguments.of(message(REJECTION, null, REJECTION.reason(), REJECTION.expiresAt()), REJECTION),
                Arguments.of(message(EXPIRATION, null, EXPIRATION.reason(), null), EXPIRATION),
                Arguments.of(message(INVALIDATION, null, INVALIDATION.reason(), null), INVALIDATION),
                Arguments.of(message(CANCELLATION, 8251L, CANCELLATION.reason(), null), CANCELLATION));
    }

    // Without these it cannot be deduplicated nor applied, and another version or type has no meaning in v1.
    @ParameterizedTest
    @MethodSource("messagesThatAreNotAnEventOfTheContract")
    void rejectsAMessageThatIsNotAnEventOfTheContract(AppointmentActionMessage message, String problem) {
        assertThatThrownBy(() -> appointmentActionMessageMapper.toDomain(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(problem);
    }

    static Stream<Arguments> messagesThatAreNotAnEventOfTheContract() {
        return Stream.of(
                Arguments.of(new AppointmentActionMessage(null, "AppointmentConfirmed", PROCESS_ID, 8251L, null, null,
                        1), "eventId"),
                Arguments.of(new AppointmentActionMessage(" ", "AppointmentConfirmed", PROCESS_ID, 8251L, null, null,
                        1), "eventId"),
                Arguments.of(new AppointmentActionMessage("event-1", "AppointmentConfirmed", null, 8251L, null, null,
                        1), "reservationProcessId"),
                Arguments.of(new AppointmentActionMessage("event-1", null, PROCESS_ID, 8251L, null, null, 1),
                        "eventType"),
                Arguments.of(new AppointmentActionMessage("event-1", "AppointmentRescheduled", PROCESS_ID, null, null,
                        null, 1), "AppointmentRescheduled"),
                Arguments.of(new AppointmentActionMessage("event-1", "AppointmentConfirmed", PROCESS_ID, 8251L, null,
                        null, 2), "schemaVersion"),
                Arguments.of(new AppointmentActionMessage("event-1", "AppointmentConfirmed", PROCESS_ID, 8251L, null,
                        null, null), "schemaVersion"));
    }

    private static AppointmentActionMessage message(AppointmentAction action, Long reservationId, String reason,
                                                    Instant expiresAt) {
        return new AppointmentActionMessage(action.eventId(), action.getClass().getSimpleName(),
                action.reservationProcessId(), reservationId, reason, expiresAt, 1);
    }
}
