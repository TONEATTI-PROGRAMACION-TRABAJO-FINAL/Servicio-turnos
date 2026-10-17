package com.toneatti.turnos.user.application.service;

import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.domain.ports.in.RegisterUserUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private RegisterUserUseCase registerUserUseCase;

    @InjectMocks
    private UserService userService;

    @Test
    void registerUserDelegatesToTheUseCase() {
        User request = User.builder().email("juan@example.com").build();
        User registered = User.builder().externalId("external-id").email("juan@example.com").build();
        when(registerUserUseCase.registerUser(request, "secret-password")).thenReturn(registered);

        assertThat(userService.registerUser(request, "secret-password")).isSameAs(registered);
    }
}
