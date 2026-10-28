package com.toneatti.turnos.reservation.application.usecases;

import com.toneatti.turnos.reservation.domain.model.AppointmentAction;
import com.toneatti.turnos.reservation.domain.model.AppointmentActionOutcome;
import com.toneatti.turnos.reservation.domain.model.ProcessedAppointmentAction;
import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.ports.in.ApplyAppointmentActionUseCase;
import com.toneatti.turnos.reservation.domain.ports.out.ProcessedAppointmentActionRepository;
import com.toneatti.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Applies an event of the cátedra to the local process at most once. Kafka delivers at least once (section 18.1 of
 * the integration contract), so the eventId is recorded in the same transaction as the change of the process: it is
 * recorded if and only if the change was saved. A redelivery after a crash, or the same event processed at the same
 * time by two consumers, finds it recorded or fails on its unique constraint and rolls back.
 * An event the process does not accept and one of an unknown process are recorded too, since processing them again
 * would give the same outcome.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplyAppointmentActionUseCaseImpl implements ApplyAppointmentActionUseCase {

    private final ReservationProcessRepository reservationProcessRepository;
    private final ProcessedAppointmentActionRepository processedAppointmentActionRepository;
    private final Clock clock;

    @Override
    @Transactional
    public AppointmentActionOutcome applyAppointmentAction(AppointmentAction action) {
        if (processedAppointmentActionRepository.existsByEventId(action.eventId())) {
            log.info("Event {} of reservation process {} was already processed", action.eventId(),
                    action.reservationProcessId());
            return AppointmentActionOutcome.DUPLICATE;
        }
        AppointmentActionOutcome outcome = reservationProcessRepository
                .findByReservationProcessId(action.reservationProcessId())
                .map(process -> apply(process, action))
                .orElseGet(() -> {
                    log.warn("Event {} is about reservation process {}, which does not exist locally",
                            action.eventId(), action.reservationProcessId());
                    return AppointmentActionOutcome.UNKNOWN_PROCESS;
                });
        processedAppointmentActionRepository.create(new ProcessedAppointmentAction(null, action.eventId(),
                action.getClass().getSimpleName(), action.reservationProcessId(), outcome, clock.instant()));
        return outcome;
    }

    private AppointmentActionOutcome apply(ReservationProcess process, AppointmentAction action) {
        if (!process.apply(action, clock.instant())) {
            log.warn("Ignored event {} ({}) of reservation process {} in {}", action.eventId(),
                    action.getClass().getSimpleName(), process.getId(), process.getStatus());
            return AppointmentActionOutcome.LATE;
        }
        reservationProcessRepository.update(process.getId(), process)
                .orElseThrow(() -> new IllegalStateException("Reservation process " + process.getId() + " vanished"));
        log.info("Applied event {} to reservation process {}: now {}", action.eventId(), process.getId(),
                process.getStatus());
        return AppointmentActionOutcome.APPLIED;
    }
}
