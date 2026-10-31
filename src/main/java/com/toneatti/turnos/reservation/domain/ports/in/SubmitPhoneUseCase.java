package com.toneatti.turnos.reservation.domain.ports.in;

import com.toneatti.turnos.reservation.domain.model.ReservationProcess;

import java.util.Optional;

public interface SubmitPhoneUseCase {
    Optional<ReservationProcess> submitPhone(String userExternalId, Long id, String phoneNumber);
}
