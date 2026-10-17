package com.toneatti.turnos.user.infrastructure.persistence.mapper;

import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    void returnsNullForNullInput() {
        assertThat(userMapper.toEntity(null)).isNull();
        assertThat(userMapper.toDomainModel(null)).isNull();
    }

    @Test
    void mapsEveryFieldBothWays() {
        User user = User.builder()
                .id(7L)
                .externalId("3f1c2b8e-0000-4000-8000-000000000001")
                .email("juan@example.com")
                .passwordHash("hashed-password")
                .firstName("Juan")
                .lastName("Perez")
                .createdAt(Instant.parse("2026-09-10T14:30:00Z"))
                .build();

        UserEntity entity = userMapper.toEntity(user);
        User mappedBack = userMapper.toDomainModel(entity);

        assertThat(entity).usingRecursiveComparison().isEqualTo(user);
        assertThat(mappedBack).usingRecursiveComparison().isEqualTo(user);
    }
}
