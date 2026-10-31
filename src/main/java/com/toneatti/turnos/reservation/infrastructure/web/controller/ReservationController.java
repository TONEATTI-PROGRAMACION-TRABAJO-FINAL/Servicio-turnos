package com.toneatti.turnos.reservation.infrastructure.web.controller;

import com.toneatti.turnos.reservation.application.exception.ConcurrentReservationUpdateException;
import com.toneatti.turnos.reservation.application.exception.HoldExpiredException;
import com.toneatti.turnos.reservation.application.exception.InvalidPhoneNumberException;
import com.toneatti.turnos.reservation.application.exception.PhoneNotRequestedException;
import com.toneatti.turnos.reservation.application.exception.PhoneSubmissionUnavailableException;
import com.toneatti.turnos.reservation.application.exception.ReservationException;
import com.toneatti.turnos.reservation.application.exception.ReservationExpiredException;
import com.toneatti.turnos.reservation.application.exception.ReservationInProgressException;
import com.toneatti.turnos.reservation.application.exception.ReservationNotWaitingForPhoneException;
import com.toneatti.turnos.reservation.application.exception.ReservationPendingException;
import com.toneatti.turnos.reservation.application.exception.ReservationRejectedException;
import com.toneatti.turnos.reservation.application.exception.ReservationUnavailableException;
import com.toneatti.turnos.reservation.application.exception.SlotInThePastException;
import com.toneatti.turnos.reservation.application.exception.UnexpectedCatedraRejectionException;
import com.toneatti.turnos.reservation.application.service.ReservationService;
import com.toneatti.turnos.reservation.domain.model.StartedReservation;
import com.toneatti.turnos.reservation.infrastructure.web.dto.PhoneNumberRequest;
import com.toneatti.turnos.reservation.infrastructure.web.dto.ReservationRequest;
import com.toneatti.turnos.reservation.infrastructure.web.dto.ReservationResponse;
import com.toneatti.turnos.reservation.infrastructure.web.mapper.ReservationDtoMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

import static com.toneatti.turnos.config.ProblemResponses.problem;

@Slf4j
@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    private final ReservationService reservationService;
    private final ReservationDtoMapper reservationDtoMapper;

    // Spring Security answers 401 before reaching here without a valid user JWT. The patient is the subject of the
    // token; the body only chooses the slot. 201 for a new process, 200 when an active one of the slot was resumed.
    @PostMapping
    public ResponseEntity<ReservationResponse> startReservation(@AuthenticationPrincipal Jwt jwt,
                                                                @Valid @RequestBody ReservationRequest request) {
        try {
            StartedReservation started = reservationService.startReservation(jwt.getSubject(),
                    request.getProfessionalId(), request.getDate(), request.getStartTime());
            ReservationResponse response = reservationDtoMapper.toResponse(started.process());
            if (started.resumed()) {
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.created(URI.create("/reservations/" + response.getId())).body(response);
        } catch (ReservationException e) {
            throw problem(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
        } catch (SlotInThePastException e) {
            throw problem(HttpStatus.BAD_REQUEST, "SLOT_IN_THE_PAST", "The slot already started");
        } catch (ReservationRejectedException e) {
            throw problem(statusOf(e.getCode()), e.getCode(), "The reservation was rejected");
        } catch (HoldExpiredException e) {
            throw problem(HttpStatus.GONE, "HOLD_EXPIRED", "The hold of the slot expired");
        } catch (ReservationInProgressException | ConcurrentReservationUpdateException e) {
            throw problem(HttpStatus.CONFLICT, "RESERVATION_IN_PROGRESS",
                    "Another request is reserving this slot for the user");
        } catch (ReservationPendingException e) {
            // Already logged with its cause. Starting the same reservation again resumes this process.
            ResponseStatusException pending = problem(HttpStatus.SERVICE_UNAVAILABLE, "RESERVATION_PENDING",
                    "The reservation is not confirmed yet; try again later");
            pending.getBody().setProperty("reservationId", e.getId());
            throw pending;
        } catch (ReservationUnavailableException e) {
            log.warn("Reservations are unavailable: check the technical integration of the catedra", e);
            throw problem(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                    "Reservations are temporarily unavailable");
        } catch (UnexpectedCatedraRejectionException e) {
            log.error("Catedra rejected a reservation request it should have accepted", e);
            throw problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error");
        }
    }

    // Another user's process answers 404 as well, so its existence is not revealed. Spring answers 400 when the id
    // is not a number.
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(@AuthenticationPrincipal Jwt jwt,
                                                                  @PathVariable Long id) {
        try {
            return ResponseEntity.ok(reservationDtoMapper.toResponse(
                    reservationService.getReservationById(jwt.getSubject(), id)));
        } catch (ReservationException e) {
            throw reservationNotFound();
        }
    }

    // 202: the cátedra answers later through its events, which the app sees with GET /reservations/{id}. Another
    // user's process answers 404 as well.
    @PostMapping("/{id}/phone")
    public ResponseEntity<ReservationResponse> submitPhone(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                           @Valid @RequestBody PhoneNumberRequest request) {
        try {
            return ResponseEntity.accepted().body(reservationDtoMapper.toResponse(
                    reservationService.submitPhone(jwt.getSubject(), id, request.getPhoneNumber())));
        } catch (ReservationException e) {
            throw reservationNotFound();
        } catch (InvalidPhoneNumberException e) {
            throw problem(HttpStatus.BAD_REQUEST, "INVALID_PHONE_NUMBER",
                    "The phone number must have 7 to 15 digits and may start with +");
        } catch (ReservationNotWaitingForPhoneException e) {
            throw problem(HttpStatus.CONFLICT, "RESERVATION_NOT_WAITING_FOR_PHONE",
                    "The reservation is not waiting for a phone number");
        } catch (PhoneNotRequestedException e) {
            throw problem(HttpStatus.CONFLICT, "PHONE_NOT_REQUESTED_YET",
                    "The phone number was not requested yet; try again in a moment");
        } catch (ReservationExpiredException e) {
            throw problem(HttpStatus.GONE, "RESERVATION_EXPIRED", "The reservation expired");
        } catch (ConcurrentReservationUpdateException e) {
            throw problem(HttpStatus.CONFLICT, "RESERVATION_IN_PROGRESS",
                    "The reservation changed meanwhile; check it and try again");
        } catch (PhoneSubmissionUnavailableException e) {
            // Already logged with its cause. The process keeps waiting for the phone, so it can be sent again.
            throw problem(HttpStatus.SERVICE_UNAVAILABLE, "PHONE_SUBMISSION_UNAVAILABLE",
                    "The phone number could not be sent; try again");
        }
    }

    // Only for this controller: a missing or invalid field, or a body that is not JSON or has a date or time in
    // another format.
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ProblemDetail> invalidBody(Exception e) {
        String detail = e instanceof MethodArgumentNotValidException invalid
                ? String.join("; ", invalid.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage()).sorted().toList())
                : "The body is not valid JSON or has a value in the wrong format";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setProperty("code", VALIDATION_ERROR);
        return ResponseEntity.badRequest().body(problem);
    }

    private static HttpStatus statusOf(String code) {
        return switch (code) {
            case "PROFESSIONAL_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "PROFESSIONAL_DISABLED", "INVALID_SLOT" -> HttpStatus.BAD_REQUEST;
            case "HOLD_NOT_FOUND" -> HttpStatus.GONE;
            // SLOT_ALREADY_RESERVED and SLOT_ALREADY_HELD.
            default -> HttpStatus.CONFLICT;
        };
    }

    private static ResponseStatusException reservationNotFound() {
        return problem(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "Reservation not found");
    }
}
