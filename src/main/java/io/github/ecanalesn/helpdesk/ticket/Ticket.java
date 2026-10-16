package io.github.ecanalesn.helpdesk.ticket;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Ticket de soporte. Los cambios de estado solo se hacen con los métodos de negocio
 * ({@link #start}, {@link #resolve}, {@link #reopen}), que registran las marcas de tiempo
 * de las que salen los indicadores de respuesta y resolución.
 */
@Entity
@Table(name = "tickets")
public class Ticket {

    public static final int TITLE_MAX = 120;
    public static final int DESCRIPTION_MAX = 2000;
    public static final int PERSON_MAX = 80;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Bloqueo optimista: dos técnicos no pueden pisarse los cambios sobre el mismo ticket. */
    @Version
    private long version;

    @Column(nullable = false, length = TITLE_MAX)
    private String title;

    @Column(length = DESCRIPTION_MAX)
    private String description;

    @Column(nullable = false, length = PERSON_MAX)
    private String requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;

    @Column(length = PERSON_MAX)
    private String assignee;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant firstResponseAt;

    private Instant resolvedAt;

    protected Ticket() {
        // requerido por JPA
    }

    public Ticket(String title, String description, String requester, Category category, Priority priority,
                  Instant createdAt) {
        this.title = requireText(title, "El título");
        this.description = description == null ? "" : description.strip();
        this.requester = requireText(requester, "El solicitante");
        this.category = Objects.requireNonNull(category, "category");
        this.priority = Objects.requireNonNull(priority, "priority");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.status = Status.ABIERTO;
    }

    /** Un técnico se asigna el ticket: cuenta como primera respuesta. */
    public void start(String assignee, Instant at) {
        if (status != Status.ABIERTO) {
            throw new TicketStateException("Solo se puede empezar un ticket abierto (#" + id + " está " + status.getLabel().toLowerCase() + ")");
        }
        requireNotBefore(at, createdAt);
        this.assignee = requireText(assignee, "El técnico asignado");
        this.firstResponseAt = at;
        this.status = Status.EN_CURSO;
    }

    /** Resuelve el ticket. Si se resuelve sin pasar por "en curso", la resolución es también la primera respuesta. */
    public void resolve(Instant at) {
        if (status == Status.RESUELTO) {
            throw new TicketStateException("El ticket #" + id + " ya está resuelto");
        }
        requireNotBefore(at, firstResponseAt != null ? firstResponseAt : createdAt);
        if (firstResponseAt == null) {
            firstResponseAt = at;
        }
        this.resolvedAt = at;
        this.status = Status.RESUELTO;
    }

    /** Vuelve a "en curso" un ticket resuelto; la primera respuesta se conserva. */
    public void reopen() {
        if (status != Status.RESUELTO) {
            throw new TicketStateException("Solo se puede reabrir un ticket resuelto");
        }
        this.resolvedAt = null;
        this.status = Status.EN_CURSO;
    }

    public Optional<Duration> responseTime() {
        return Optional.ofNullable(firstResponseAt).map(at -> Duration.between(createdAt, at));
    }

    public Optional<Duration> resolutionTime() {
        return Optional.ofNullable(resolvedAt).map(at -> Duration.between(createdAt, at));
    }

    public Instant responseDeadline() {
        return createdAt.plus(priority.getResponseTarget());
    }

    public Instant resolutionDeadline() {
        return createdAt.plus(priority.getResolutionTarget());
    }

    /** ¿Se respondió (o, si sigue sin respuesta, se está todavía) dentro del objetivo? */
    public boolean responseSlaMet(Instant now) {
        return !(firstResponseAt != null ? firstResponseAt : now).isAfter(responseDeadline());
    }

    /** ¿Se resolvió (o, si sigue pendiente, se está todavía) dentro del objetivo? */
    public boolean resolutionSlaMet(Instant now) {
        return !(resolvedAt != null ? resolvedAt : now).isAfter(resolutionDeadline());
    }

    public boolean canStart() {
        return status == Status.ABIERTO;
    }

    public boolean canResolve() {
        return status != Status.RESUELTO;
    }

    public boolean canReopen() {
        return status == Status.RESUELTO;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value.strip();
    }

    private static void requireNotBefore(Instant at, Instant reference) {
        Objects.requireNonNull(at, "at");
        if (at.isBefore(reference)) {
            throw new IllegalArgumentException("La fecha " + at + " es anterior a " + reference);
        }
    }

    // ------------------------------------------------------------------ getters

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getRequester() {
        return requester;
    }

    public Category getCategory() {
        return category;
    }

    public Priority getPriority() {
        return priority;
    }

    public Status getStatus() {
        return status;
    }

    public String getAssignee() {
        return assignee;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getFirstResponseAt() {
        return firstResponseAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
