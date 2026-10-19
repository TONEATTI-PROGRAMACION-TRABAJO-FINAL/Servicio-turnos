package com.toneatti.turnos.user.domain.ports.out;

import com.toneatti.turnos.user.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    User create(User user);
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);

}
