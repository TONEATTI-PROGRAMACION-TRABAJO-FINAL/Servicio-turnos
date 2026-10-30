package com.toneatti.turnos.reservation.domain.ports.in;

import com.toneatti.turnos.reservation.domain.model.AppointmentAction;
import com.toneatti.turnos.reservation.domain.model.AppointmentActionOutcome;

public interface ApplyAppointmentActionUseCase {
    AppointmentActionOutcome applyAppointmentAction(AppointmentAction action);
}
