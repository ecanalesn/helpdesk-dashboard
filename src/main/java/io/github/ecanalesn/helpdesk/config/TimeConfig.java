package io.github.ecanalesn.helpdesk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class TimeConfig {

    /** Toda la aplicación pide la hora a este bean: en los tests se sustituye por un reloj fijo. */
    @Bean
    public Clock clock(HelpdeskProperties properties) {
        return Clock.system(properties.timeZone());
    }
}
