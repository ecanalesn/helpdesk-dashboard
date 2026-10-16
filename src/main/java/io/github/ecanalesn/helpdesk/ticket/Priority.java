package io.github.ecanalesn.helpdesk.ticket;

import java.time.Duration;

/**
 * Prioridad del ticket y sus objetivos de servicio (SLA), en horas naturales.
 * Los valores son de ejemplo: se ajustan al acuerdo de servicio de cada equipo.
 */
public enum Priority {

    CRITICA("Crítica", "danger", Duration.ofHours(1), Duration.ofHours(4)),
    ALTA("Alta", "warning", Duration.ofHours(4), Duration.ofHours(24)),
    MEDIA("Media", "primary", Duration.ofHours(8), Duration.ofHours(72)),
    BAJA("Baja", "secondary", Duration.ofHours(24), Duration.ofHours(120));

    private final String label;
    private final String badge;
    private final Duration responseTarget;
    private final Duration resolutionTarget;

    Priority(String label, String badge, Duration responseTarget, Duration resolutionTarget) {
        this.label = label;
        this.badge = badge;
        this.responseTarget = responseTarget;
        this.resolutionTarget = resolutionTarget;
    }

    public String getLabel() {
        return label;
    }

    /** Clase de color de Bootstrap para la etiqueta. */
    public String getBadge() {
        return badge;
    }

    /** Tiempo máximo hasta la primera respuesta (paso a "en curso"). */
    public Duration getResponseTarget() {
        return responseTarget;
    }

    /** Tiempo máximo hasta la resolución. */
    public Duration getResolutionTarget() {
        return resolutionTarget;
    }
}
