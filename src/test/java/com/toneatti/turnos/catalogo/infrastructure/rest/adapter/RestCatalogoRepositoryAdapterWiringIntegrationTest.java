package com.toneatti.turnos.catalogo.infrastructure.rest.adapter;

import com.toneatti.turnos.support.FakeHttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * With two RestClient beans in the context, the catalogo adapter must get the catalogo one:
 * requests reach Servicio-catalogo and never carry the cátedra technical JWT.
 */
@SpringBootTest
@ActiveProfiles("test")
class RestCatalogoRepositoryAdapterWiringIntegrationTest {

    private static final FakeHttpServer CATALOGO = startCatalogo();

    @Autowired
    private RestCatalogoRepositoryAdapter adapter;

    @DynamicPropertySource
    static void catalogoBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("catalogo.base-url", CATALOGO::baseUrl);
    }

    @AfterAll
    static void stopCatalogo() {
        CATALOGO.close();
    }

    @Test
    void callsTheCatalogoWithoutTheCatedraJwt() {
        CATALOGO.respond("/professional-categories", 200, "[]");

        assertThat(adapter.findAllCategories()).isEmpty();
        assertThat(CATALOGO.lastRequestUri().getPath()).isEqualTo("/professional-categories");
        assertThat(CATALOGO.lastRequestHeader(HttpHeaders.AUTHORIZATION)).isNull();
    }

    private static FakeHttpServer startCatalogo() {
        try {
            return FakeHttpServer.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
