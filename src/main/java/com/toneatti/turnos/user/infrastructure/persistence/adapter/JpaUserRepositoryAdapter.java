package com.toneatti.turnos.user.infrastructure.persistence.adapter;

import com.toneatti.turnos.user.application.exception.EmailAlreadyRegisteredException;
import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.domain.ports.out.UserRepository;
import com.toneatti.turnos.user.infrastructure.persistence.entity.UserEntity;
import com.toneatti.turnos.user.infrastructure.persistence.mapper.UserMapper;
import com.toneatti.turnos.user.infrastructure.persistence.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;
    private final UserMapper userMapper;

    @Override
    public User create(User user) {
        UserEntity userEntity = userMapper.toEntity(user);
        try {
            UserEntity savedEntity = jpaUserRepository.saveAndFlush(userEntity);
            return userMapper.toDomainModel(savedEntity);
        } catch (DataIntegrityViolationException e) {
            // Two concurrent registrations can both pass the existsByEmail check; the unique constraint decides.
            if (jpaUserRepository.existsByEmail(user.email())) {
                throw new EmailAlreadyRegisteredException(user.email());
            }
            throw e;
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email).map(userMapper::toDomainModel);
    }

    @Override
    public Optional<User> findByExternalId(String externalId) {
        return jpaUserRepository.findByExternalId(externalId).map(userMapper::toDomainModel);
    }
}
