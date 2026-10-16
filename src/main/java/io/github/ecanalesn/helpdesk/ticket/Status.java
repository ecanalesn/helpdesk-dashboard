package io.github.ecanalesn.helpdesk.ticket;

/** Estado del ticket: ABIERTO → EN_CURSO → RESUELTO (y RESUELTO → EN_CURSO al reabrir). */
public enum Status {

    ABIERTO("Abierto", "secondary"),
    EN_CURSO("En curso", "info"),
    RESUELTO("Resuelto", "success");

    private final String label;
    private final String badge;

    Status(String label, String badge) {
        this.label = label;
        this.badge = badge;
    }

    public String getLabel() {
        return label;
    }

    public String getBadge() {
        return badge;
    }
}
