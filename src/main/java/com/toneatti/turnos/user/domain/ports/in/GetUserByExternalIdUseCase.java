package com.toneatti.turnos.user.domain.ports.in;

import com.toneatti.turnos.user.domain.model.User;

import java.util.Optional;

public interface GetUserByExternalIdUseCase {
    Optional<User> getUserByExternalId(String externalId);
}
