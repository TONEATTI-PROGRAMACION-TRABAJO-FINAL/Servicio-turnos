package com.toneatti.turnos.catedra.infrastructure.rest.adapter;

import com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException;
import com.toneatti.turnos.catedra.domain.exception.CatedraUnavailableException;
import com.toneatti.turnos.catedra.domain.model.Hold;
import com.toneatti.turnos.catedra.domain.model.Occupancy;
import com.toneatti.turnos.catedra.domain.ports.out.CatedraRepository;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraHoldRequest;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraHoldResponse;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraOccupanciesResponse;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraProblemResponse;
import com.toneatti.turnos.catedra.infrastructure.rest.mapper.CatedraRestMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
        String request = "POST " + HOLDS_PATH;
        CatedraHoldResponse body = call(request, () ->
                catedraRestClient.post()
                        .uri(HOLDS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new CatedraHoldRequest(professionalId, date, startTime, externalPatientId))
                        .retrieve()
                        .body(CatedraHoldResponse.class));
        // A 201 without the hold cannot be used, and the hold may exist: the caller has to reconcile as after a timeout.
        if (body == null || body.holdId() == null || body.reservationProcessId() == null) {
            throw new CatedraUnavailableException("Catedra answered " + request + " without a hold", null);
        }
        return catedraRestMapper.toDomainModel(body);
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
