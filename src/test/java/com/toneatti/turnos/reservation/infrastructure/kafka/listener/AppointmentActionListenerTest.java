package com.toneatti.turnos.reservation.infrastructure.kafka.listener;

import com.toneatti.turnos.reservation.application.service.ReservationService;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction;
import com.toneatti.turnos.reservation.infrastructure.kafka.mapper.AppointmentActionMessageMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import static com.toneatti.turnos.reservation.domain.model.AppointmentActionFixtures.CONFIRMATION;
import static com.toneatti.turnos.reservation.infrastructure.kafka.AppointmentActionMessages.json;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentActionListenerTest {

    @Mock
    private ReservationService reservationService;

    private AppointmentActionListener listener;

    @BeforeEach
    void setUp() {
        listener = new AppointmentActionListener(reservationService, new AppointmentActionMessageMapper(),
                JsonMapper.builder().build());
    }

    // The JSON of the contract, with a field added later that is ignored.
    @ParameterizedTest
    @FieldSource("com.toneatti.turnos.reservation.domain.model.AppointmentActionFixtures#ALL")
    void appliesEachEventOfTheContract(AppointmentAction action) {
        listener.onAppointmentAction(recordOf(json(action)));

        verify(reservationService).applyAppointmentAction(action);
    }

    // Logged and skipped, without throwing: retrying it would fail the same way and block the partition.
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "not json", "null", "[]", "{\"eventType\":\"AppointmentConfirmed\"}",
            "{\"eventId\":\"e-1\",\"eventType\":\"AppointmentConfirmed\",\"reservationProcessId\":\"p-1\","
                    + "\"schemaVersion\":1,\"expiresAt\":\"tomorrow\"}"})
    void skipsAnUnreadableMessage(String value) {
        listener.onAppointmentAction(recordOf(value));

        verifyNoInteractions(reservationService);
    }

    // The error handler of the container retries it.
    @Test
    void throwsWhenApplyingAReadableEventFails() {
        RuntimeException failure = new RuntimeException("database down");
        when(reservationService.applyAppointmentAction(CONFIRMATION)).thenThrow(failure);

        assertThatThrownBy(() -> listener.onAppointmentAction(recordOf(json(CONFIRMATION)))).isSameAs(failure);
    }

    private static ConsumerRecord<String, String> recordOf(String value) {
        return new ConsumerRecord<>("alumnos.turnos.acciones.test", 0, 5L, "process-a92b47d0", value);
    }
}
