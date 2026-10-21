package com.toneatti.turnos.user.application.usecases;

import com.toneatti.turnos.user.domain.model.AccessToken;
import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.domain.ports.in.LoginUserUseCase;
import com.toneatti.turnos.user.domain.ports.out.AccessTokenIssuer;
import com.toneatti.turnos.user.domain.ports.out.PasswordHasher;
import com.toneatti.turnos.user.domain.ports.out.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LoginUserUseCaseImpl implements LoginUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    // Compared against when the email does not exist, so both cases cost one hash comparison.
    private final String unknownUserPasswordHash;

    public LoginUserUseCaseImpl(UserRepository userRepository, PasswordHasher passwordHasher,
                                AccessTokenIssuer accessTokenIssuer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.accessTokenIssuer = accessTokenIssuer;
        this.unknownUserPasswordHash = passwordHasher.hash("unknown-user-password");
    }

    @Override
    public Optional<AccessToken> loginUser(String email, String rawPassword) {
        Optional<User> user = userRepository.findByEmail(User.normalizeEmail(email));
        // The response time must not reveal whether the email is registered.
        String passwordHash = user.map(User::passwordHash).orElse(unknownUserPasswordHash);
        boolean passwordMatches = passwordHasher.matches(rawPassword, passwordHash);
        return user.filter(found -> passwordMatches).map(accessTokenIssuer::issue);
    }

}
