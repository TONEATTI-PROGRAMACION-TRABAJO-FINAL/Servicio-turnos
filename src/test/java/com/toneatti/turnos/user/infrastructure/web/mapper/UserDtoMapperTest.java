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

        assertThat(user.email()).isEqualTo("juan@example.com");
        assertThat(user.firstName()).isEqualTo("Juan");
        assertThat(user.lastName()).isEqualTo("Perez");
        assertThat(user.passwordHash()).isNull();
        assertThat(user.externalId()).isNull();
    }

    @Test
    void toResponseExposesOnlyPublicFields() {
        User user = new User(
                7L,
                "3f1c2b8e-0000-4000-8000-000000000001",
                "juan@example.com",
                "hashed-password",
                "Juan",
                "Perez",
                Instant.parse("2026-09-10T14:30:00Z"));

        UserResponse response = userDtoMapper.toResponse(user);

        assertThat(response.getExternalId()).isEqualTo("3f1c2b8e-0000-4000-8000-000000000001");
        assertThat(response.getEmail()).isEqualTo("juan@example.com");
        assertThat(response.getFirstName()).isEqualTo("Juan");
        assertThat(response.getLastName()).isEqualTo("Perez");
    }
}
