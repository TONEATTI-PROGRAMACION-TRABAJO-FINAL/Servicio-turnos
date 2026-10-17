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
        String email = user.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
        User newUser = User.builder()
                // Sent to the cátedra as externalPatientId: must never change.
                .externalId(UUID.randomUUID().toString())
                .email(email)
                .passwordHash(passwordHasher.hash(rawPassword))
                .firstName(user.getFirstName().trim())
                .lastName(user.getLastName().trim())
                .createdAt(Instant.now())
                .build();
        return userRepository.create(newUser);
    }

}
