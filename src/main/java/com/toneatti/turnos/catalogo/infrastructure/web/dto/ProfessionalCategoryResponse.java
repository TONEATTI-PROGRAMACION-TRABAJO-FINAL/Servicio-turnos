package com.toneatti.turnos.catalogo.infrastructure.web.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalCategoryResponse {
    private Long id;
    private String name;
    private String description;
}
