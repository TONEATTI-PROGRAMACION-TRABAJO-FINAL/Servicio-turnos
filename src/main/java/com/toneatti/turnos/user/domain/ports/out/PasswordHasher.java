package com.toneatti.turnos.user.domain.ports.out;

public interface PasswordHasher {

    String hash(String rawPassword);

}
