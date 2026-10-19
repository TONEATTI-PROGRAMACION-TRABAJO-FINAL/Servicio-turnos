package com.toneatti.turnos.user.infrastructure.web.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String externalId;
    private String email;
    private String firstName;
    private String lastName;
}
