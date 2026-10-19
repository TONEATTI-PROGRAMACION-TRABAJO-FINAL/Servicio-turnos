package com.toneatti.turnos.user.application.usecases;

import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.domain.ports.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserByExternalIdUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetUserByExternalIdUseCaseImpl getUserByExternalIdUseCase;

    @Test
    void returnsTheUserWithThatExternalId() {
        User user = new User(7L, "external-id", "juan@example.com", "hashed-password", "Juan", "Perez", null);
        when(userRepository.findByExternalId("external-id")).thenReturn(Optional.of(user));

        assertThat(getUserByExternalIdUseCase.getUserByExternalId("external-id")).contains(user);
    }

    @Test
    void returnsEmptyWhenNoUserHasThatExternalId() {
        when(userRepository.findByExternalId("missing-id")).thenReturn(Optional.empty());

        assertThat(getUserByExternalIdUseCase.getUserByExternalId("missing-id")).isEmpty();
    }
}
