package com.toneatti.turnos.catalogo.infrastructure.rest.adapter;

import com.toneatti.turnos.catalogo.application.exception.CatalogoUnavailableException;
import com.toneatti.turnos.catalogo.domain.model.Professional;
import com.toneatti.turnos.catalogo.domain.model.ProfessionalCategory;
import com.toneatti.turnos.catalogo.domain.ports.out.CatalogoRepository;
import com.toneatti.turnos.catalogo.infrastructure.rest.dto.CatalogoProfessionalCategoryResponse;
import com.toneatti.turnos.catalogo.infrastructure.rest.dto.CatalogoProfessionalResponse;
import com.toneatti.turnos.catalogo.infrastructure.rest.mapper.CatalogoRestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Calls the endpoints described in Servicio-catalogo/docs/API-CATALOGO.md with the {@link RestClient}
 * defined in {@link com.toneatti.turnos.catalogo.infrastructure.rest.config.CatalogoRestClientConfig}.
 */
@Component
@RequiredArgsConstructor
public class RestCatalogoRepositoryAdapter implements CatalogoRepository {

    private static final ParameterizedTypeReference<List<CatalogoProfessionalCategoryResponse>> CATEGORIES =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<CatalogoProfessionalResponse>> PROFESSIONALS =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient catalogoRestClient;
    private final CatalogoRestMapper catalogoRestMapper;

    @Override
    public List<ProfessionalCategory> findAllCategories() {
        List<CatalogoProfessionalCategoryResponse> body = call("GET /professional-categories", () ->
                catalogoRestClient.get().uri("/professional-categories").retrieve().body(CATEGORIES));
        return mapAll(body, catalogoRestMapper::toDomainModel);
    }

    @Override
    public List<Professional> findProfessionalsByCategoryId(Long categoryId) {
        List<CatalogoProfessionalResponse> body = call("GET /professionals", () ->
                catalogoRestClient.get()
                        .uri(uri -> uri.path("/professionals")
                                .queryParamIfPresent("categoryId", Optional.ofNullable(categoryId))
                                .build())
                        .retrieve()
                        .body(PROFESSIONALS));
        return mapAll(body, catalogoRestMapper::toDomainModel);
    }

    // Error statuses, timeouts, refused connections and unreadable bodies all surface as RestClientException.
    private static <T> T call(String request, Supplier<T> exchange) {
        try {
            return exchange.get();
        } catch (RestClientException e) {
            throw new CatalogoUnavailableException("Catalogo request failed: " + request, e);
        }
    }

    private static <S, T> List<T> mapAll(List<S> body, Function<S, T> mapper) {
        if (body == null) {
            return List.of();
        }
        return body.stream().map(mapper).toList();
    }
}
