package com.toneatti.turnos.catalogo.infrastructure.rest.adapter;

import com.toneatti.turnos.catalogo.application.exception.CatalogoUnavailableException;
import com.toneatti.turnos.catalogo.domain.model.ProfessionalCategory;
import com.toneatti.turnos.catalogo.infrastructure.rest.FakeCatalogoServer;
import com.toneatti.turnos.catalogo.infrastructure.rest.config.CatalogoRestClientConfig;
import com.toneatti.turnos.catalogo.infrastructure.rest.mapper.CatalogoRestMapper;
import com.toneatti.turnos.config.CatalogoServiceProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestCatalogoRepositoryAdapterTest {

    private static final Duration READ_TIMEOUT = Duration.ofMillis(500);
    private static final String CATEGORIES_PATH = "/professional-categories";

    private FakeCatalogoServer catalogo;
    private RestCatalogoRepositoryAdapter adapter;

    @BeforeEach
    void startCatalogo() throws IOException {
        catalogo = FakeCatalogoServer.start();
        adapter = adapterFor(catalogo.baseUrl());
    }

    @AfterEach
    void stopCatalogo() {
        catalogo.close();
    }

    @Test
    void findsAllCategories() {
        catalogo.respond(CATEGORIES_PATH, 200, """
                [{"id":1,"name":"Clinica medica","description":"Atencion clinica general"},
                 {"id":2,"name":"Pediatria","description":null}]
                """);

        assertThat(adapter.findAllCategories()).containsExactly(
                new ProfessionalCategory(1L, "Clinica medica", "Atencion clinica general"),
                new ProfessionalCategory(2L, "Pediatria", null));
    }

    @Test
    void findsNoCategoriesWhenTheCatalogoHasNone() {
        catalogo.respond(CATEGORIES_PATH, 200, "[]");

        assertThat(adapter.findAllCategories()).isEmpty();
    }

    @Test
    void findsNoCategoriesWhenTheBodyIsEmpty() {
        catalogo.respond(CATEGORIES_PATH, 200, "");

        assertThat(adapter.findAllCategories()).isEmpty();
    }

    @Test
    void failsAsUnavailableOnAServerError() {
        catalogo.respond(CATEGORIES_PATH, 500, "{\"status\":500,\"title\":\"Internal Server Error\"}");

        assertThatThrownBy(adapter::findAllCategories)
                .isInstanceOf(CatalogoUnavailableException.class)
                .hasMessageContaining("GET " + CATEGORIES_PATH)
                .hasCauseInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void failsAsUnavailableOnAClientError() {
        catalogo.respond(CATEGORIES_PATH, 400, "{\"status\":400,\"title\":\"Bad Request\"}");

        assertThatThrownBy(adapter::findAllCategories)
                .isInstanceOf(CatalogoUnavailableException.class)
                .hasCauseInstanceOf(HttpClientErrorException.class);
    }

    @Test
    void failsAsUnavailableWhenTheCatalogoTakesLongerThanTheReadTimeout() {
        catalogo.respondAfter(CATEGORIES_PATH, READ_TIMEOUT.multipliedBy(4), 200, "[]");

        assertThatThrownBy(adapter::findAllCategories)
                .isInstanceOf(CatalogoUnavailableException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
    }

    @Test
    void failsAsUnavailableWhenTheConnectionIsRefused() throws IOException {
        RestCatalogoRepositoryAdapter unreachable = adapterFor(FakeCatalogoServer.unreachableBaseUrl());

        assertThatThrownBy(unreachable::findAllCategories)
                .isInstanceOf(CatalogoUnavailableException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
    }

    @Test
    void failsAsUnavailableWhenTheBodyIsNotJson() {
        catalogo.respond(CATEGORIES_PATH, 200, "not json");

        assertThatThrownBy(adapter::findAllCategories)
                .isInstanceOf(CatalogoUnavailableException.class);
    }

    private static RestCatalogoRepositoryAdapter adapterFor(String baseUrl) {
        CatalogoServiceProperties properties =
                new CatalogoServiceProperties(baseUrl, Duration.ofSeconds(1), READ_TIMEOUT);
        return new RestCatalogoRepositoryAdapter(
                new CatalogoRestClientConfig().catalogoRestClient(properties), new CatalogoRestMapper());
    }
}
