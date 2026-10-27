package com.toneatti.turnos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Clock in the time zone of the place of attention: the schedules and slots use local times (section 4 of the
 * integration contract), so "now" must be read in that zone. Injected so tests can fix it.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${turnos.time-zone}") ZoneId timeZone) {
        return Clock.system(timeZone);
    }
}
