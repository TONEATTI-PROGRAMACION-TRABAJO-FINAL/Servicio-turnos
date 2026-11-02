package com.toneatti.turnos.catedra.infrastructure.rest.adapter;

import com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException;
import com.toneatti.turnos.catedra.domain.exception.CatedraUnavailableException;
import com.toneatti.turnos.catedra.domain.model.Appointment;
import com.toneatti.turnos.catedra.domain.model.AppointmentCancellation;
import com.toneatti.turnos.catedra.domain.model.AppointmentPage;
import com.toneatti.turnos.catedra.domain.model.AppointmentStatus;
import com.toneatti.turnos.catedra.domain.model.AppointmentSummary;
import com.toneatti.turnos.catedra.domain.model.Hold;
import com.toneatti.turnos.catedra.domain.model.HoldConfirmation;
import com.toneatti.turnos.catedra.domain.model.Occupancy;
import com.toneatti.turnos.catedra.domain.ports.out.CatedraRepository;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraAppointmentListResponse;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraAppointmentResponse;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraCancellationRequest;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraCancellationResponse;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraHoldConfirmationRequest;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraHoldConfirmationResponse;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraHoldRequest;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraHoldResponse;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraOccupanciesResponse;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraProblemResponse;
import com.toneatti.turnos.catedra.infrastructure.rest.mapper.CatedraRestMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Calls the endpoints of the cátedra REST API (section 6 of the integration contract) with the {@link RestClient}
 * defined in {@link com.toneatti.turnos.catedra.infrastructure.rest.config.CatedraRestClientConfig}.
 */
@Slf4j
@Component
public class RestCatedraRepositoryAdapter implements CatedraRepository {

    private static final String OCCUPANCIES_PATH = "/api/appointment-occupancies";
    private static final String HOLDS_PATH = "/api/appointment-holds";
    private static final String CONFIRM_HOLD_PATH = HOLDS_PATH + "/{holdId}/confirm";
    private static final String APPOINTMENTS_PATH = "/api/appointments";
    private static final String CANCEL_APPOINTMENT_PATH = APPOINTMENTS_PATH + "/{reservationProcessId}/cancel";
    // A patient has a handful of reservations in a few days, so the first page is enough and the pagination headers
    // (X-Total-Count, Link) are not followed.
    private static final int APPOINTMENTS_PAGE_SIZE = 100;
    private static final String TOTAL_COUNT_HEADER = "X-Total-Count";

    private final RestClient catedraRestClient;
    private final CatedraRestMapper catedraRestMapper;

    // Explicit constructor so @Qualifier picks the cátedra client among the RestClient beans.
    public RestCatedraRepositoryAdapter(@Qualifier("catedraRestClient") RestClient catedraRestClient,
                                        CatedraRestMapper catedraRestMapper) {
        this.catedraRestClient = catedraRestClient;
        this.catedraRestMapper = catedraRestMapper;
    }

    @Override
    public List<Occupancy> findOccupancies(Long professionalId, LocalDate from, LocalDate to) {
        CatedraOccupanciesResponse body = call("GET " + OCCUPANCIES_PATH, () ->
                catedraRestClient.get()
                        .uri(uri -> uri.path(OCCUPANCIES_PATH)
                                .queryParam("professionalId", professionalId)
                                .queryParam("from", from)
                                .queryParam("to", to)
                                .build())
                        .retrieve()
                        .body(CatedraOccupanciesResponse.class));
        // An empty list is a valid answer (section 8).
        if (body == null || body.occupancies() == null) {
            return List.of();
        }
        return body.occupancies().stream().map(catedraRestMapper::toDomainModel).toList();
    }

    @Override
    public Hold createHold(Long professionalId, LocalDate date, LocalTime startTime, String externalPatientId) {
        CatedraHoldResponse body = post(HOLDS_PATH,
                new CatedraHoldRequest(professionalId, date, startTime, externalPatientId),
                CatedraHoldResponse.class, hold -> hold.holdId() != null && hold.reservationProcessId() != null);
        return catedraRestMapper.toDomainModel(body);
    }

    @Override
    public HoldConfirmation confirmHold(String holdId, String externalPatientId,
                                        String patientFirstName, String patientLastName) {
        CatedraHoldConfirmationResponse body = post(CONFIRM_HOLD_PATH,
                new CatedraHoldConfirmationRequest(externalPatientId, patientFirstName, patientLastName),
                CatedraHoldConfirmationResponse.class, confirmation -> confirmation.reservationProcessId() != null,
                holdId);
        return catedraRestMapper.toDomainModel(body);
    }

    // A 200 that does not say CANCELLED cannot be trusted either way, so it fails as unavailable and the caller
    // reconciles.
    @Override
    public AppointmentCancellation cancelAppointment(String reservationProcessId, String reason) {
        CatedraCancellationResponse body = post(CANCEL_APPOINTMENT_PATH, new CatedraCancellationRequest(reason),
                CatedraCancellationResponse.class, cancellation -> cancellation.reservationProcessId() != null
                        && cancellation.status() == AppointmentStatus.CANCELLED,
                reservationProcessId);
        return catedraRestMapper.toDomainModel(body);
    }

    @Override
    public List<Appointment> findAppointments(String externalPatientId, LocalDate from, LocalDate to) {
        CatedraAppointmentResponse[] body = call("GET " + APPOINTMENTS_PATH, () ->
                catedraRestClient.get()
                        .uri(uri -> uri.path(APPOINTMENTS_PATH)
                                .queryParam("externalPatientId", externalPatientId)
                                .queryParam("from", from)
                                .queryParam("to", to)
                                .queryParam("page", 0)
                                .queryParam("size", APPOINTMENTS_PAGE_SIZE)
                                .build())
                        .retrieve()
                        .body(CatedraAppointmentResponse[].class));
        if (body == null) {
            return List.of();
        }
        return Arrays.stream(body).filter(Objects::nonNull).map(catedraRestMapper::toDomainModel).toList();
    }

    @Override
    public AppointmentPage findAppointmentsPage(String externalPatientId, LocalDate from, LocalDate to,
                                                AppointmentStatus status, int page, int size) {
        ResponseEntity<CatedraAppointmentListResponse[]> response = call("GET " + APPOINTMENTS_PATH, () ->
                catedraRestClient.get()
                        .uri(uriBuilder -> appointmentsPageUri(uriBuilder, externalPatientId, from, to, status,
                                page, size))
                        .retrieve()
                        .toEntity(CatedraAppointmentListResponse[].class));
        CatedraAppointmentListResponse[] body = response.getBody();
        List<AppointmentSummary> content = body == null ? List.of() :
                Arrays.stream(body).filter(Objects::nonNull).map(catedraRestMapper::toDomainModel).toList();
        return new AppointmentPage(content, totalCountOf(response, content.size()));
    }

    private static URI appointmentsPageUri(UriBuilder uriBuilder, String externalPatientId, LocalDate from,
                                                     LocalDate to, AppointmentStatus status, int page, int size) {
        UriBuilder builder = uriBuilder.path(APPOINTMENTS_PATH)
                .queryParam("externalPatientId", externalPatientId);
        if (from != null) {
            builder = builder.queryParam("from", from);
        }
        if (to != null) {
            builder = builder.queryParam("to", to);
        }
        if (status != null) {
            builder = builder.queryParam("status", status);
        }
        return builder.queryParam("page", page).queryParam("size", size).build();
    }

    // The cátedra reports the total count independently of the page (section 11); a missing or unreadable header
    // falls back to the size of this page, which is only wrong when there is more than one page.
    private static long totalCountOf(ResponseEntity<?> response, int pageSize) {
        String header = response.getHeaders().getFirst(TOTAL_COUNT_HEADER);
        if (header == null) {
            return pageSize;
        }
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException e) {
            return pageSize;
        }
    }

    // A POST has effects in the cátedra. A success without a usable body cannot be used, and the effect may have
    // happened, so it fails as unavailable: the caller has to reconcile as after a timeout.
    private <T> T post(String path, Object requestBody, Class<T> responseType, Predicate<T> complete,
                       Object... uriVariables) {
        String request = "POST " + path;
        T body = call(request, () ->
                catedraRestClient.post()
                        .uri(path, uriVariables)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(responseType));
        if (body == null || !complete.test(body)) {
            throw new CatedraUnavailableException("Catedra answered " + request + " without a usable body", null);
        }
        return body;
    }

    // A 4xx with a functional code is a rejection the caller can act on; everything else means the cátedra
    // could not serve the request. Error statuses, timeouts, refused connections and unreadable bodies all
    // surface as RestClientException.
    private static <T> T call(String request, Supplier<T> exchange) {
        try {
            return exchange.get();
        } catch (HttpClientErrorException e) {
            String code = codeOf(e);
            if (code != null) {
                throw new CatedraRequestRejectedException(e.getStatusCode().value(), code,
                        "Catedra rejected " + request + " with " + code, e);
            }
            if (e.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)
                    || e.getStatusCode().isSameCodeAs(HttpStatus.FORBIDDEN)) {
                // Spring Security answers these without a code: the technical JWT is missing, expired or revoked.
                log.warn("Catedra answered {} to {}: check CATEDRA_JWT", e.getStatusCode().value(), request);
            }
            throw new CatedraUnavailableException("Catedra request failed: " + request, e);
        } catch (RestClientException e) {
            throw new CatedraUnavailableException("Catedra request failed: " + request, e);
        }
    }

    private static String codeOf(HttpClientErrorException e) {
        try {
            CatedraProblemResponse problem = e.getResponseBodyAs(CatedraProblemResponse.class);
            return problem == null || problem.code() == null || problem.code().isBlank() ? null : problem.code();
        } catch (RuntimeException unreadable) {
            return null;
        }
    }
}
