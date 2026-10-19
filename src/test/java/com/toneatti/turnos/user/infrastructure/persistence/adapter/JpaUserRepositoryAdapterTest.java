package com.toneatti.turnos.user.infrastructure.persistence.adapter;

import com.toneatti.turnos.user.application.exception.EmailAlreadyRegisteredException;
import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.infrastructure.persistence.entity.UserEntity;
import com.toneatti.turnos.user.infrastructure.persistence.mapper.UserMapper;
import com.toneatti.turnos.user.infrastructure.persistence.repository.JpaUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaUserRepositoryAdapterTest {

    @Mock
    private JpaUserRepository jpaUserRepository;

    private JpaUserRepositoryAdapter adapter() {
        return new JpaUserRepositoryAdapter(jpaUserRepository, new UserMapper());
    }

    private final User user = new User(null, null, "juan@example.com", null, null, null, null);

    @Test
    void translatesAUniqueViolationOnTheEmailIntoEmailAlreadyRegistered() {
        when(jpaUserRepository.saveAndFlush(any(UserEntity.class)))
                .thenThrow(new DataIntegrityViolationException("unique violation"));
        when(jpaUserRepository.existsByEmail("juan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> adapter().create(user))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void rethrowsAnIntegrityViolationNotCausedByTheEmail() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException("other constraint");
        when(jpaUserRepository.saveAndFlush(any(UserEntity.class))).thenThrow(violation);
        when(jpaUserRepository.existsByEmail("juan@example.com")).thenReturn(false);

        assertThatThrownBy(() -> adapter().create(user)).isSameAs(violation);
    }

    @Test
    void findByEmailMapsTheStoredUser() {
        when(jpaUserRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(storedEntity()));

        assertThat(adapter().findByEmail("juan@example.com"))
                .hasValueSatisfying(found -> {
                    assertThat(found.id()).isEqualTo(7L);
                    assertThat(found.externalId()).isEqualTo("3f1c2b8e-0000-4000-8000-000000000001");
                    assertThat(found.passwordHash()).isEqualTo("hashed-password");
                });
    }

    @Test
    void findByEmailReturnsEmptyWhenThereIsNoUser() {
        when(jpaUserRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThat(adapter().findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void findByExternalIdMapsTheStoredUser() {
        when(jpaUserRepository.findByExternalId("3f1c2b8e-0000-4000-8000-000000000001"))
                .thenReturn(Optional.of(storedEntity()));

        assertThat(adapter().findByExternalId("3f1c2b8e-0000-4000-8000-000000000001"))
                .hasValueSatisfying(found -> assertThat(found.email()).isEqualTo("juan@example.com"));
    }

    @Test
    void findByExternalIdReturnsEmptyWhenThereIsNoUser() {
        when(jpaUserRepository.findByExternalId("missing-id")).thenReturn(Optional.empty());

        assertThat(adapter().findByExternalId("missing-id")).isEmpty();
    }

    private static UserEntity storedEntity() {
        return UserEntity.builder()
                .id(7L)
                .externalId("3f1c2b8e-0000-4000-8000-000000000001")
                .email("juan@example.com")
                .passwordHash("hashed-password")
                .firstName("Juan")
                .lastName("Perez")
                .createdAt(Instant.parse("2026-09-10T14:30:00Z"))
                .build();
    }
}
