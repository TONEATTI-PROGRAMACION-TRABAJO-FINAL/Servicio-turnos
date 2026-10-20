package com.toneatti.turnos.user.domain.ports.in;

import com.toneatti.turnos.user.domain.model.AccessToken;

import java.util.Optional;

public interface LoginUserUseCase {
    Optional<AccessToken> loginUser(String email, String rawPassword);
}
