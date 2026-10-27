package com.toneatti.turnos.availability.application.exception;

public class ProfessionalDisabledException extends RuntimeException {
    public ProfessionalDisabledException(Long professionalId) {
        super("Professional is disabled: " + professionalId);
    }
}
