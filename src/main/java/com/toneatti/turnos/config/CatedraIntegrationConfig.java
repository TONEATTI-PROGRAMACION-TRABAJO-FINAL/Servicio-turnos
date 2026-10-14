package com.toneatti.turnos.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({CatedraIntegrationProperties.class, CatalogoServiceProperties.class})
public class CatedraIntegrationConfig {
}
