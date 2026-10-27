package com.toneatti.turnos.catalogo.infrastructure.rest.config;

import com.toneatti.turnos.config.CatalogoServiceProperties;
import com.toneatti.turnos.config.RestClientBuilders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CatalogoRestClientConfig {

    @Bean
    public RestClient catalogoRestClient(CatalogoServiceProperties properties) {
        return RestClientBuilders.withTimeouts(
                        properties.baseUrl(), properties.connectTimeout(), properties.readTimeout())
                .build();
    }
}
