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
public class UserRequest {

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email must be valid")
    @Size(max = 254, message = "Email must have at most 254 characters")
    private String email;

    // The maximum only keeps huge passwords from making every Argon2id hash more expensive.
    @NotBlank(message = "Password is mandatory")
    @Size(min = 8, max = 128, message = "Password must have between 8 and 128 characters")
    private String password;

    // Same limits the cátedra applies to patientFirstName and patientLastName when confirming a hold.
    @NotBlank(message = "First name is mandatory")
    @Size(min = 2, max = 100, message = "First name must have between 2 and 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is mandatory")
    @Size(min = 2, max = 100, message = "Last name must have between 2 and 100 characters")
    private String lastName;
}
