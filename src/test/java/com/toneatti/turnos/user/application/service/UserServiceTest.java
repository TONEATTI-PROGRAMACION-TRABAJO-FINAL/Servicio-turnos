package com.toneatti.turnos.user.application.service;

import com.toneatti.turnos.user.application.exception.UserException;
import com.toneatti.turnos.user.domain.model.AccessToken;
import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.domain.ports.in.GetUserByExternalIdUseCase;
import com.toneatti.turnos.user.domain.ports.in.LoginUserUseCase;
import com.toneatti.turnos.user.domain.ports.in.RegisterUserUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private RegisterUserUseCase registerUserUseCase;

    @Mock
    private LoginUserUseCase loginUserUseCase;

    @Mock
    private GetUserByExternalIdUseCase getUserByExternalIdUseCase;

    @InjectMocks
    private UserService userService;

    @Test
    void registerUserDelegatesToTheUseCase() {
        User request = new User(null, null, "juan@example.com", null, null, null, null);
        User registered = new User(null, "external-id", "juan@example.com", null, null, null, null);
        when(registerUserUseCase.registerUser(request, "secret-password")).thenReturn(registered);

        assertThat(userService.registerUser(request, "secret-password")).isSameAs(registered);
    }

    @Test
    void loginUserReturnsTheTokenOfTheUseCase() {
        AccessToken token = new AccessToken("token", Instant.EPOCH, Instant.EPOCH.plusSeconds(60));
        when(loginUserUseCase.loginUser("juan@example.com", "secret-password")).thenReturn(Optional.of(token));

        assertThat(userService.loginUser("juan@example.com", "secret-password")).isSameAs(token);
    }

    @Test
    void loginUserThrowsUserExceptionWhenTheCredentialsAreInvalid() {
        when(loginUserUseCase.loginUser("juan@example.com", "wrong-password")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loginUser("juan@example.com", "wrong-password"))
                .isInstanceOf(UserException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void getUserByExternalIdReturnsTheUserOfTheUseCase() {
        User user = new User(7L, "external-id", "juan@example.com", null, null, null, null);
        when(getUserByExternalIdUseCase.getUserByExternalId("external-id")).thenReturn(Optional.of(user));

        assertThat(userService.getUserByExternalId("external-id")).isSameAs(user);
    }

    @Test
    void getUserByExternalIdThrowsUserExceptionWhenTheUserDoesNotExist() {
        when(getUserByExternalIdUseCase.getUserByExternalId("missing-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByExternalId("missing-id"))
                .isInstanceOf(UserException.class);
    }
}
