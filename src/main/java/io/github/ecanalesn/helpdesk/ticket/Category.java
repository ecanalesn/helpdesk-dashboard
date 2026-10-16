package io.github.ecanalesn.helpdesk.ticket;

/** Área de la incidencia: las herramientas que usan los equipos de desarrollo a lo largo del SDLC. */
public enum Category {

    IDE("IDE (IntelliJ, VS Code, Eclipse)", "IDE"),
    CONTROL_VERSIONES("Control de versiones (Git, GitHub, GitLab)", "Git"),
    BUILD("Build y dependencias (Maven)", "Maven"),
    INTEGRACION_CONTINUA("Integración continua", "CI/CD"),
    ACCESOS("Accesos y licencias", "Accesos"),
    OTROS("Otros", "Otros");

    private final String label;
    private final String shortLabel;

    Category(String label, String shortLabel) {
        this.label = label;
        this.shortLabel = shortLabel;
    }

    public String getLabel() {
        return label;
    }

    /** Nombre corto para las gráficas. */
    public String getShortLabel() {
        return shortLabel;
    }
}
