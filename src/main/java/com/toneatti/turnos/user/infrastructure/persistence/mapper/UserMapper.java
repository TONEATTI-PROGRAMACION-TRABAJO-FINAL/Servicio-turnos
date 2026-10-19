package com.toneatti.turnos.user.infrastructure.persistence.mapper;

import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }
        return UserEntity.builder()
                .id(user.id())
                .externalId(user.externalId())
                .email(user.email())
                .passwordHash(user.passwordHash())
                .firstName(user.firstName())
                .lastName(user.lastName())
                .createdAt(user.createdAt())
                .build();
    }

    public User toDomainModel(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return new User(
                entity.getId(),
                entity.getExternalId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getCreatedAt());
    }
}
