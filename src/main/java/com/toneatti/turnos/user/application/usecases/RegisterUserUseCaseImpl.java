package com.toneatti.turnos.user.application.usecases;

import com.toneatti.turnos.user.application.exception.EmailAlreadyRegisteredException;
import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.domain.ports.in.RegisterUserUseCase;
import com.toneatti.turnos.user.domain.ports.out.PasswordHasher;
import com.toneatti.turnos.user.domain.ports.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    @Override
    public User registerUser(User user, String rawPassword) {
        String email = user.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
        User newUser = new User(
                null,
                // Sent to the cátedra as externalPatientId: must never change.
                UUID.randomUUID().toString(),
                email,
                passwordHasher.hash(rawPassword),
                user.firstName().trim(),
                user.lastName().trim(),
                Instant.now());
        return userRepository.create(newUser);
    }

}
