package com.toneatti.turnos.user.domain.ports.out;

import com.toneatti.turnos.user.domain.model.AccessToken;
import com.toneatti.turnos.user.domain.model.User;

public interface AccessTokenIssuer {

    AccessToken issue(User user);

}
