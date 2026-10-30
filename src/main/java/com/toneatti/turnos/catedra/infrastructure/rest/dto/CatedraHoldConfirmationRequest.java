package com.toneatti.turnos.catedra.infrastructure.rest.dto;

public record CatedraHoldConfirmationRequest(
        String externalPatientId,
        String patientFirstName,
        String patientLastName
) {
}
