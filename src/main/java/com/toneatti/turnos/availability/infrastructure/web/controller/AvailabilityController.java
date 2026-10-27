package com.toneatti.turnos.availability.infrastructure.web.controller;

import com.toneatti.turnos.availability.application.exception.AvailabilityException;
import com.toneatti.turnos.availability.application.exception.AvailabilityUnavailableException;
import com.toneatti.turnos.availability.application.exception.DateRangeTooLongException;
import com.toneatti.turnos.availability.application.exception.InvalidDateRangeException;
import com.toneatti.turnos.availability.application.exception.ProfessionalDisabledException;
import com.toneatti.turnos.availability.application.service.AvailabilityService;
import com.toneatti.turnos.availability.infrastructure.web.dto.AvailableSlotResponse;
import com.toneatti.turnos.availability.infrastructure.web.mapper.AvailabilityDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/available-slots")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final AvailabilityDtoMapper availabilityDtoMapper;

    // Spring Security answers 401 before reaching here without a valid user JWT, and Spring answers 400 when a
    // parameter is missing, the id is not a number or a date is not yyyy-MM-dd.
    @GetMapping
    public ResponseEntity<List<AvailableSlotResponse>> getAvailableSlots(
            @RequestParam Long professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            return ResponseEntity.ok(availabilityService.getAvailableSlots(professionalId, from, to).stream()
                    .map(availabilityDtoMapper::toResponse)
                    .toList());
        } catch (InvalidDateRangeException e) {
            throw problem(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", e.getMessage());
        } catch (DateRangeTooLongException e) {
            throw problem(HttpStatus.BAD_REQUEST, "DATE_RANGE_TOO_LONG", e.getMessage());
        } catch (AvailabilityException e) {
            throw problem(HttpStatus.NOT_FOUND, "PROFESSIONAL_NOT_FOUND", "Professional not found");
        } catch (ProfessionalDisabledException e) {
            throw problem(HttpStatus.BAD_REQUEST, "PROFESSIONAL_DISABLED", "Professional is disabled");
        } catch (AvailabilityUnavailableException e) {
            // The cause stays in the log only: the app must not see details of Servicio-catalogo or the cátedra.
            log.warn("Could not calculate the availability of professional {}", professionalId, e);
            throw problem(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                    "Availability is temporarily unavailable");
        }
    }

    // Rendered as application/problem+json with a stable "code", the same convention as the cátedra (section 13).
    private static ResponseStatusException problem(HttpStatus status, String code, String detail) {
        ResponseStatusException exception = new ResponseStatusException(status, detail);
        exception.getBody().setProperty("code", code);
        return exception;
    }
}
