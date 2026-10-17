package com.toneatti.turnos.user.application.usecases;

import com.toneatti.turnos.user.application.exception.EmailAlreadyRegisteredException;
import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.domain.ports.out.PasswordHasher;
import com.toneatti.turnos.user.domain.ports.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseImplTest {

    private static final String NORMALIZED_EMAIL = "juan.perez@example.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @InjectMocks
    private RegisterUserUseCaseImpl registerUserUseCase;

    private final User request =
            new User(null, null, "  Juan.Perez@Example.COM ", null, " Juan ", " Perez ", null);

    @Test
    void storesTheHashAndNeverTheRawPassword() {
        givenTheEmailIsFree();

        User created = registerUserUseCase.registerUser(request, "secret-password");

        assertThat(created.passwordHash()).isEqualTo("hashed-password");
    }

    @Test
    void normalizesEmailAndNames() {
        givenTheEmailIsFree();

        User created = registerUserUseCase.registerUser(request, "secret-password");

        assertThat(created.email()).isEqualTo(NORMALIZED_EMAIL);
        assertThat(created.firstName()).isEqualTo("Juan");
        assertThat(created.lastName()).isEqualTo("Perez");
    }

    @Test
    void generatesAUuidExternalIdAndCreationInstant() {
        givenTheEmailIsFree();

        User created = registerUserUseCase.registerUser(request, "secret-password");

        assertThat(UUID.fromString(created.externalId())).isNotNull();
        assertThat(created.externalId()).hasSizeLessThanOrEqualTo(100);
        assertThat(created.createdAt()).isNotNull();
        assertThat(created.id()).isNull();
    }

    @Test
    void generatesADifferentExternalIdForEachUser() {
        givenTheEmailIsFree();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        registerUserUseCase.registerUser(request, "secret-password");
        registerUserUseCase.registerUser(request, "secret-password");

        verify(userRepository, times(2)).create(captor.capture());
        assertThat(captor.getAllValues().get(0).externalId())
                .isNotEqualTo(captor.getAllValues().get(1).externalId());
    }

    @Test
    void rejectsAnEmailAlreadyRegisteredIgnoringCase() {
        when(userRepository.existsByEmail(NORMALIZED_EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> registerUserUseCase.registerUser(request, "secret-password"))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(passwordHasher, never()).hash(anyString());
        verify(userRepository, never()).create(any(User.class));
    }

    private void givenTheEmailIsFree() {
        when(userRepository.existsByEmail(NORMALIZED_EMAIL)).thenReturn(false);
        when(passwordHasher.hash("secret-password")).thenReturn("hashed-password");
        when(userRepository.create(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
