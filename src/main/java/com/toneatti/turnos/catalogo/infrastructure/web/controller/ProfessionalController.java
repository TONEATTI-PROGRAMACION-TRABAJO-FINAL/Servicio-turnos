package com.toneatti.turnos.catalogo.infrastructure.web.controller;

import com.toneatti.turnos.catalogo.application.exception.CatalogoException;
import com.toneatti.turnos.catalogo.application.service.CatalogoService;
import com.toneatti.turnos.catalogo.domain.exception.CatalogoUnavailableException;
import com.toneatti.turnos.catalogo.infrastructure.web.dto.ProfessionalResponse;
import com.toneatti.turnos.catalogo.infrastructure.web.mapper.CatalogoDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.toneatti.turnos.config.ProblemResponses.problem;

@Slf4j
@RestController
@RequestMapping("/professionals")
@RequiredArgsConstructor
public class ProfessionalController {

    private final CatalogoService catalogoService;
    private final CatalogoDtoMapper catalogoDtoMapper;

    // Spring Security answers 401 before reaching here without a valid user JWT, and Spring answers 400 when
    // categoryId is not a number.
    @GetMapping
    public ResponseEntity<List<ProfessionalResponse>> getProfessionalsByCategory(
            @RequestParam(required = false) Long categoryId) {
        try {
            return ResponseEntity.ok(catalogoService.getProfessionalsByCategory(categoryId).stream()
                    .map(catalogoDtoMapper::toResponse)
                    .toList());
        } catch (CatalogoUnavailableException e) {
            // The cause stays in the log only: the app must not see details of Servicio-catalogo.
            log.warn("Could not list the professionals of category {}", categoryId, e);
            throw problem(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                    "The catalog is temporarily unavailable");
        }
    }

    // A disabled professional answers 200 with enabled false; Spring answers 400 when the id is not a number.
    @GetMapping("/{id}")
    public ResponseEntity<ProfessionalResponse> getProfessionalById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(catalogoDtoMapper.toResponse(catalogoService.getProfessionalById(id)));
        } catch (CatalogoException e) {
            throw problem(HttpStatus.NOT_FOUND, "PROFESSIONAL_NOT_FOUND", "Professional not found");
        } catch (CatalogoUnavailableException e) {
            log.warn("Could not get professional {}", id, e);
            throw problem(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                    "The catalog is temporarily unavailable");
        }
    }
}
