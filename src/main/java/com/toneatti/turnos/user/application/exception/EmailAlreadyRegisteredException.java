package com.toneatti.turnos.user.application.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException(String email) {
        super("Email already registered: " + email);
    }
}
