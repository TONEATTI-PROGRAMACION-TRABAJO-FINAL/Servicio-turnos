package com.toneatti.turnos.reservation.infrastructure.web.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * The body is optional, and so is {@code reason}; at most 500 characters, as the cátedra accepts (section 12).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationRequest {

    @Size(max = 500, message = "Reason must have at most 500 characters")
    private String reason;
}
