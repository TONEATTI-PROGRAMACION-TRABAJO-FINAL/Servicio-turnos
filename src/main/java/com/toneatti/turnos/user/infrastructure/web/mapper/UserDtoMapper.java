package com.toneatti.turnos.user.infrastructure.web.mapper;

import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.infrastructure.web.dto.UserRequest;
import com.toneatti.turnos.user.infrastructure.web.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserDtoMapper {

    // The raw password is not part of the domain model: the controller passes it to the service separately.
    public User toDomain(UserRequest request) {
        if (request == null) {
            return null;
        }
        return User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();
    }

    public UserResponse toResponse(User domain) {
        if (domain == null) {
            return null;
        }
        return UserResponse.builder()
                .externalId(domain.getExternalId())
                .email(domain.getEmail())
                .firstName(domain.getFirstName())
                .lastName(domain.getLastName())
                .build();
    }
}
