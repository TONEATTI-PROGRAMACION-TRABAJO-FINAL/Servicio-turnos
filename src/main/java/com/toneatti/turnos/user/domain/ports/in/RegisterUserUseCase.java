package com.toneatti.turnos.user.domain.ports.in;

import com.toneatti.turnos.user.domain.model.User;

public interface RegisterUserUseCase {
    User registerUser(User user, String rawPassword);
}
