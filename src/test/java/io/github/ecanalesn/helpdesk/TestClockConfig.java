package io.github.ecanalesn.helpdesk;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** Reloj fijo para los tests: 16/10/2026 a las 10:00 en Madrid. */
@TestConfiguration(proxyBeanMethods = false)
public class TestClockConfig {

    public static final ZoneId ZONE = ZoneId.of("Europe/Madrid");
    public static final Instant NOW = Instant.parse("2026-10-16T08:00:00Z");

    @Bean
    @Primary
    public Clock fixedClock() {
        return Clock.fixed(NOW, ZONE);
    }
}
