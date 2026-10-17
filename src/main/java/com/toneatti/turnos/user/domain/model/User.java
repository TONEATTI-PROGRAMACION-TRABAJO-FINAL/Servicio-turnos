package com.toneatti.turnos.user.domain.model;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String externalId;
    private String email;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private Instant createdAt;
}
