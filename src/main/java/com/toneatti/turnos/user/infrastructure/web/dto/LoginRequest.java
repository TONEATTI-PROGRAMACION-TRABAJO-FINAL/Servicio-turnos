package com.toneatti.turnos.user.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email must be valid")
    private String email;

    // No minimum length: that policy applies when registering, not when logging in.
    @NotBlank(message = "Password is mandatory")
    @Size(max = 128, message = "Password must have at most 128 characters")
    private String password;
}
