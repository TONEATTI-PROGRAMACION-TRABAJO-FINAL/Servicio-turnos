package com.toneatti.turnos.user.application.usecases;

import com.toneatti.turnos.user.domain.model.AccessToken;
import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.domain.ports.out.AccessTokenIssuer;
import com.toneatti.turnos.user.domain.ports.out.PasswordHasher;
import com.toneatti.turnos.user.domain.ports.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUserUseCaseImplTest {

    private static final String UNKNOWN_USER_HASH = "unknown-user-hash";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private AccessTokenIssuer accessTokenIssuer;

    private LoginUserUseCaseImpl loginUserUseCase;

    private final User user = new User(
            7L, "3f1c2b8e-0000-4000-8000-000000000001", "juan.perez@example.com", "stored-hash",
            "Juan", "Perez", Instant.parse("2026-09-10T14:30:00Z"));

    private final AccessToken token =
            new AccessToken("signed-token", Instant.EPOCH, Instant.EPOCH.plusSeconds(60));

    @BeforeEach
    void setUp() {
        // The use case hashes a placeholder password once, when it is built.
        when(passwordHasher.hash(anyString())).thenReturn(UNKNOWN_USER_HASH);
        loginUserUseCase = new LoginUserUseCaseImpl(userRepository, passwordHasher, accessTokenIssuer);
    }

    @Test
    void returnsATokenWhenThePasswordMatches() {
        when(userRepository.findByEmail("juan.perez@example.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("secret-password", "stored-hash")).thenReturn(true);
        when(accessTokenIssuer.issue(user)).thenReturn(token);

        assertThat(loginUserUseCase.loginUser("juan.perez@example.com", "secret-password")).contains(token);
    }

    @Test
    void looksTheEmailUpNormalized() {
        when(userRepository.findByEmail("juan.perez@example.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("secret-password", "stored-hash")).thenReturn(true);
        when(accessTokenIssuer.issue(user)).thenReturn(token);

        assertThat(loginUserUseCase.loginUser("  Juan.Perez@Example.COM ", "secret-password")).contains(token);
    }

    @Test
    void returnsEmptyWhenThePasswordDoesNotMatch() {
        when(userRepository.findByEmail("juan.perez@example.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong-password", "stored-hash")).thenReturn(false);

        assertThat(loginUserUseCase.loginUser("juan.perez@example.com", "wrong-password")).isEmpty();
        verify(accessTokenIssuer, never()).issue(any(User.class));
    }

    @Test
    void returnsEmptyForAnUnknownEmailAfterComparingAgainstThePlaceholderHash() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThat(loginUserUseCase.loginUser("nobody@example.com", "secret-password")).isEmpty();
        // Same work as a wrong password, so the response time does not reveal the email is not registered.
        verify(passwordHasher).matches("secret-password", UNKNOWN_USER_HASH);
        verify(accessTokenIssuer, never()).issue(any(User.class));
    }
}
