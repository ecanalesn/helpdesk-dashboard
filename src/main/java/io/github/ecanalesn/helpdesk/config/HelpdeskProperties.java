package io.github.ecanalesn.helpdesk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.ZoneId;

/**
 * Propiedades {@code helpdesk.*} de application.properties.
 *
 * @param timeZone zona horaria con la que se agrupan los días y los meses del panel
 * @param demoData datos de ejemplo que se cargan al arrancar con la base de datos vacía
 */
@ConfigurationProperties("helpdesk")
public record HelpdeskProperties(
        @DefaultValue("Europe/Madrid") ZoneId timeZone,
        @DefaultValue DemoData demoData) {

    public record DemoData(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("300") int tickets,
            @DefaultValue("90") int days,
            @DefaultValue("42") long seed) {
    }
}
