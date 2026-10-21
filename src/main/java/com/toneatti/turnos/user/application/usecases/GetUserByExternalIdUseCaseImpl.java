package com.toneatti.turnos.user.application.usecases;

import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.domain.ports.in.GetUserByExternalIdUseCase;
import com.toneatti.turnos.user.domain.ports.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserByExternalIdUseCaseImpl implements GetUserByExternalIdUseCase {

    private final UserRepository userRepository;

    @Override
    public Optional<User> getUserByExternalId(String externalId) {
        return userRepository.findByExternalId(externalId);
    }

}
