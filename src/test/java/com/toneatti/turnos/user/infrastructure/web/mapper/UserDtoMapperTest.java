package com.toneatti.turnos.user.infrastructure.web.mapper;

import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.infrastructure.web.dto.UserRequest;
import com.toneatti.turnos.user.infrastructure.web.dto.UserResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserDtoMapperTest {

    private final UserDtoMapper userDtoMapper = new UserDtoMapper();

    @Test
    void returnsNullForNullInput() {
        assertThat(userDtoMapper.toDomain(null)).isNull();
        assertThat(userDtoMapper.toResponse(null)).isNull();
    }

    @Test
    void toDomainLeavesThePasswordOutOfTheModel() {
        UserRequest request = UserRequest.builder()
                .email("juan@example.com")
                .password("secret-password")
                .firstName("Juan")
                .lastName("Perez")
                .build();

        User user = userDtoMapper.toDomain(request);

        assertThat(user.getEmail()).isEqualTo("juan@example.com");
        assertThat(user.getFirstName()).isEqualTo("Juan");
        assertThat(user.getLastName()).isEqualTo("Perez");
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getExternalId()).isNull();
    }

    @Test
    void toResponseExposesOnlyPublicFields() {
        User user = User.builder()
                .id(7L)
                .externalId("3f1c2b8e-0000-4000-8000-000000000001")
                .email("juan@example.com")
                .passwordHash("hashed-password")
                .firstName("Juan")
                .lastName("Perez")
                .createdAt(Instant.parse("2026-09-10T14:30:00Z"))
                .build();

        UserResponse response = userDtoMapper.toResponse(user);

        assertThat(response.getExternalId()).isEqualTo("3f1c2b8e-0000-4000-8000-000000000001");
        assertThat(response.getEmail()).isEqualTo("juan@example.com");
        assertThat(response.getFirstName()).isEqualTo("Juan");
        assertThat(response.getLastName()).isEqualTo("Perez");
    }
}
