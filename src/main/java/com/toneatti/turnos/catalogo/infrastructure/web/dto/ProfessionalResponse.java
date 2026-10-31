package com.toneatti.turnos.catalogo.infrastructure.web.dto;

import lombok.*;

/**
 * The same body for the list and the detail: {@code enabled} is always true in the list.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalResponse {
    private Long id;
    private Long categoryId;
    private String firstName;
    private String lastName;
    private boolean enabled;
}
