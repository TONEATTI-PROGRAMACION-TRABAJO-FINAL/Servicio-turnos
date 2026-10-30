package com.toneatti.turnos.reservation.domain.ports.out;

import com.toneatti.turnos.reservation.domain.model.ProcessedAppointmentAction;

public interface ProcessedAppointmentActionRepository {

    ProcessedAppointmentAction create(ProcessedAppointmentAction action);
    boolean existsByEventId(String eventId);

}
