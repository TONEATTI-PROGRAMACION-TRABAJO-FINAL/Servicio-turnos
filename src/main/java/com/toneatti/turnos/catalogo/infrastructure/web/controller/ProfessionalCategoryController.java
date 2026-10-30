package com.toneatti.turnos.catalogo.infrastructure.web.controller;

import com.toneatti.turnos.catalogo.application.service.CatalogoService;
import com.toneatti.turnos.catalogo.domain.exception.CatalogoUnavailableException;
import com.toneatti.turnos.catalogo.infrastructure.web.dto.ProfessionalCategoryResponse;
import com.toneatti.turnos.catalogo.infrastructure.web.mapper.CatalogoDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/professional-categories")
@RequiredArgsConstructor
public class ProfessionalCategoryController {

    private final CatalogoService catalogoService;
    private final CatalogoDtoMapper catalogoDtoMapper;

    // Spring Security answers 401 before reaching here without a valid user JWT.
    @GetMapping
    public ResponseEntity<List<ProfessionalCategoryResponse>> getAllProfessionalCategories() {
        try {
            return ResponseEntity.ok(catalogoService.getAllProfessionalCategories().stream()
                    .map(catalogoDtoMapper::toResponse)
                    .toList());
        } catch (CatalogoUnavailableException e) {
            // The cause stays in the log only: the app must not see details of Servicio-catalogo.
            log.warn("Could not list the professional categories", e);
            throw problem(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                    "The catalog is temporarily unavailable");
        }
    }

    // Rendered as application/problem+json with a stable "code", the same convention as the cátedra (section 13).
    private static ResponseStatusException problem(HttpStatus status, String code, String detail) {
        ResponseStatusException exception = new ResponseStatusException(status, detail);
        exception.getBody().setProperty("code", code);
        return exception;
    }
}
