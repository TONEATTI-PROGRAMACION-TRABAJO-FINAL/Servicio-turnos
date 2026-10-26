package com.toneatti.turnos.config;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Shared setup of the {@link RestClient}s that call other services: base URL plus connect and read timeouts,
 * so a slow or unreachable service fails fast instead of holding the request thread.
 */
public final class RestClientBuilders {

    private RestClientBuilders() {
    }

    public static RestClient.Builder withTimeouts(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory);
    }
}
