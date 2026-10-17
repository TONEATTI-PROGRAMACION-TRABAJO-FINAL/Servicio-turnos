package com.toneatti.turnos.user.domain.ports.out;

import com.toneatti.turnos.user.domain.model.User;

public interface UserRepository {

    User create(User user);
    boolean existsByEmail(String email);

}
