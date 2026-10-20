package com.toneatti.turnos.user.infrastructure.web.dto;

import com.toneatti.turnos.user.infrastructure.web.validation.PasswordWithinBcryptLimit;
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

    @NotBlank(message = "Password is mandatory")
    @Size(min = 8, message = "Password must have at least 8 characters")
    @PasswordWithinBcryptLimit
    private String password;

    // Same limits the cátedra applies to patientFirstName and patientLastName when confirming a hold.
    @NotBlank(message = "First name is mandatory")
    @Size(min = 2, max = 100, message = "First name must have between 2 and 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is mandatory")
    @Size(min = 2, max = 100, message = "Last name must have between 2 and 100 characters")
    private String lastName;
}
