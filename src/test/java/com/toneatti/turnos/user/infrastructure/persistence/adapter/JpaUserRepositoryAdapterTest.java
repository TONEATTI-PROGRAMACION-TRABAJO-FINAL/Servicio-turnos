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
}
