package com.toneatti.turnos.reservation.infrastructure.kafka.listener;

import com.toneatti.turnos.reservation.application.service.ReservationService;
import com.toneatti.turnos.reservation.domain.model.AppointmentAction;
import com.toneatti.turnos.reservation.infrastructure.kafka.dto.AppointmentActionMessage;
import com.toneatti.turnos.reservation.infrastructure.kafka.mapper.AppointmentActionMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Consumes the events of the cátedra about the reservation processes. A message that cannot be read is logged and
 * skipped: reading it again would fail the same way, and retrying it would block the events behind it in its
 * partition. A failure while applying a readable event is thrown, so the error handler of
 * {@code AppointmentActionsKafkaConfig} retries it; the offset is committed only after the listener returns.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentActionListener {

    private final ReservationService reservationService;
    private final AppointmentActionMessageMapper appointmentActionMessageMapper;
    private final JsonMapper jsonMapper;

    @KafkaListener(topics = "${catedra.kafka.appointment-actions-topic}")
    public void onAppointmentAction(ConsumerRecord<String, String> consumerRecord) {
        AppointmentAction action;
        try {
            action = appointmentActionMessageMapper.toDomain(
                    jsonMapper.readValue(consumerRecord.value(), AppointmentActionMessage.class));
            // The JSON literal null.
            if (action == null) {
                throw new IllegalArgumentException("Empty event");
            }
        } catch (JacksonException | IllegalArgumentException e) {
            log.warn("Skipped an unreadable event at {}-{}@{}: {}", consumerRecord.topic(),
                    consumerRecord.partition(), consumerRecord.offset(), e.getMessage());
            return;
        }
        reservationService.applyAppointmentAction(action);
    }
}
