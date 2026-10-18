package com.toneatti.turnos.user.application.service;

import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.domain.ports.in.RegisterUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final RegisterUserUseCase registerUserUseCase;

    public User registerUser(User user, String rawPassword) {
        return registerUserUseCase.registerUser(user, rawPassword);
    }

}
