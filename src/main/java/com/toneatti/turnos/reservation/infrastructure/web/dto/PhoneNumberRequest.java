package com.toneatti.turnos.reservation.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * The phone number as the user typed it: spaces, dashes, parentheses and dots are removed before validating it.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneNumberRequest {

    @NotBlank(message = "Phone number is mandatory")
    private String phoneNumber;
}
