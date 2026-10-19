package com.toneatti.turnos.user.application.service;

import com.toneatti.turnos.user.application.exception.UserException;
import com.toneatti.turnos.user.domain.model.AccessToken;
import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.domain.ports.in.LoginUserUseCase;
import com.toneatti.turnos.user.domain.ports.in.RegisterUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;

    public User registerUser(User user, String rawPassword) {
        return registerUserUseCase.registerUser(user, rawPassword);
    }

    public AccessToken loginUser(String email, String rawPassword) {
        return loginUserUseCase.loginUser(email, rawPassword)
                .orElseThrow(() -> new UserException("Invalid email or password"));
    }

}
