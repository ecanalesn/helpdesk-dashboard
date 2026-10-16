package io.github.ecanalesn.helpdesk.metrics;

import io.github.ecanalesn.helpdesk.metrics.DashboardMetrics.AgingTicket;
import io.github.ecanalesn.helpdesk.metrics.DashboardMetrics.CategoryCount;
import io.github.ecanalesn.helpdesk.metrics.DashboardMetrics.DailyCount;
import io.github.ecanalesn.helpdesk.metrics.DashboardMetrics.PriorityMetrics;
import io.github.ecanalesn.helpdesk.metrics.DashboardMetrics.Summary;
import io.github.ecanalesn.helpdesk.ticket.Category;
import io.github.ecanalesn.helpdesk.ticket.Priority;
import io.github.ecanalesn.helpdesk.ticket.Status;
import io.github.ecanalesn.helpdesk.ticket.Ticket;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Calcula los indicadores de un mes a partir de una lista de tickets. No accede a la base de datos
 * ni al reloj del sistema: todo entra por parámetro, así que se prueba con datos fijos.
 * <ul>
 *   <li><b>Tiempo de respuesta</b>: de la creación a la primera respuesta, para los tickets respondidos en el mes.</li>
 *   <li><b>Tiempo de resolución</b>: de la creación a la resolución, para los tickets resueltos en el mes.</li>
 *   <li><b>Cumplimiento de SLA</b>: porcentaje de esos tickets que estuvo dentro del objetivo de su prioridad.</li>
 * </ul>
 * Los tiempos son horas naturales (24×7), no horario laboral.
 */
public final class MetricsCalculator {

    private static final int OLDEST_OPEN_LIMIT = 5;

    private final ZoneId zone;

    public MetricsCalculator(ZoneId zone) {
        this.zone = zone;
    }

    /** Primer instante del mes. */
    public Instant start(YearMonth month) {
        return month.atDay(1).atStartOfDay(zone).toInstant();
    }

    /** Primer instante del mes siguiente (el periodo es [start, end)). */
    public Instant end(YearMonth month) {
        return start(month.plusMonths(1));
    }

    public DashboardMetrics calculate(List<Ticket> tickets, YearMonth month, Instant now) {
        Instant from = start(month);
        Instant to = end(month);
        // Para el mes en curso, la "foto" de pendientes se toma ahora, no a final de mes
        Instant cut = now.isBefore(to) ? now : to;

        List<Ticket> created = filter(tickets, t -> within(t.getCreatedAt(), from, to));
        List<Ticket> responded = filter(tickets, t -> within(t.getFirstResponseAt(), from, to));
        List<Ticket> resolved = filter(tickets, t -> within(t.getResolvedAt(), from, to));
        List<Ticket> backlog = filter(tickets, t -> t.getCreatedAt().isBefore(cut)
                && (t.getResolvedAt() == null || !t.getResolvedAt().isBefore(cut)));

        int inProgress = (int) backlog.stream()
                .filter(t -> t.getFirstResponseAt() != null && t.getFirstResponseAt().isBefore(cut))
                .count();
        int overdue = (int) backlog.stream().filter(t -> !t.resolutionSlaMet(cut)).count();

        Summary summary = new Summary(
                created.size(),
                resolved.size(),
                backlog.size(),
                backlog.size() - inProgress,
                inProgress,
                overdue,
                averageHours(responded, Ticket::responseTime),
                medianHours(responded, Ticket::responseTime),
                averageHours(resolved, Ticket::resolutionTime),
                medianHours(resolved, Ticket::resolutionTime),
                percentage(responded, t -> t.responseSlaMet(cut)),
                percentage(resolved, t -> t.resolutionSlaMet(cut)));

        List<PriorityMetrics> byPriority = new ArrayList<>();
        for (Priority priority : Priority.values()) {
            Predicate<Ticket> hasPriority = t -> t.getPriority() == priority;
            List<Ticket> respondedP = filter(responded, hasPriority);
            List<Ticket> resolvedP = filter(resolved, hasPriority);
            byPriority.add(new PriorityMetrics(
                    priority,
                    priority.getLabel(),
                    filter(created, hasPriority).size(),
                    resolvedP.size(),
                    averageHours(respondedP, Ticket::responseTime),
                    averageHours(resolvedP, Ticket::resolutionTime),
                    percentage(respondedP, t -> t.responseSlaMet(cut)),
                    percentage(resolvedP, t -> t.resolutionSlaMet(cut)),
                    hours(priority.getResponseTarget()),
                    hours(priority.getResolutionTarget())));
        }

        List<CategoryCount> byCategory = new ArrayList<>();
        for (Category category : Category.values()) {
            byCategory.add(new CategoryCount(category, category.getShortLabel(),
                    filter(created, t -> t.getCategory() == category).size()));
        }

        List<DailyCount> daily = new ArrayList<>();
        LocalDate lastDay = cut.equals(to) ? month.atEndOfMonth() : cut.atZone(zone).toLocalDate();
        for (LocalDate day = month.atDay(1); !day.isAfter(lastDay); day = day.plusDays(1)) {
            LocalDate d = day;
            daily.add(new DailyCount(d,
                    filter(created, t -> localDate(t.getCreatedAt()).equals(d)).size(),
                    filter(resolved, t -> localDate(t.getResolvedAt()).equals(d)).size()));
        }

        List<AgingTicket> oldestOpen = backlog.stream()
                .sorted(Comparator.comparing(Ticket::getCreatedAt))
                .limit(OLDEST_OPEN_LIMIT)
                .map(t -> new AgingTicket(t.getId() == null ? 0 : t.getId(), t.getTitle(), t.getPriority(),
                        t.getFirstResponseAt() != null && t.getFirstResponseAt().isBefore(cut) ? Status.EN_CURSO : Status.ABIERTO,
                        t.getCreatedAt(), hours(Duration.between(t.getCreatedAt(), cut)), !t.resolutionSlaMet(cut)))
                .toList();

        return new DashboardMetrics(month, summary, List.copyOf(byPriority), List.copyOf(byCategory),
                List.copyOf(daily), oldestOpen);
    }

    // ------------------------------------------------------------------ utilidades

    private static boolean within(Instant instant, Instant from, Instant to) {
        return instant != null && !instant.isBefore(from) && instant.isBefore(to);
    }

    private static List<Ticket> filter(List<Ticket> tickets, Predicate<Ticket> predicate) {
        return tickets.stream().filter(predicate).toList();
    }

    private LocalDate localDate(Instant instant) {
        return instant.atZone(zone).toLocalDate();
    }

    private static Double averageHours(List<Ticket> tickets, Function<Ticket, Optional<Duration>> metric) {
        return durations(tickets, metric).stream()
                .mapToLong(Duration::toSeconds)
                .average()
                .stream()
                .mapToObj(seconds -> round1(seconds / 3600.0))
                .findFirst()
                .orElse(null);
    }

    private static Double medianHours(List<Ticket> tickets, Function<Ticket, Optional<Duration>> metric) {
        List<Duration> sorted = durations(tickets, metric).stream().sorted().toList();
        if (sorted.isEmpty()) {
            return null;
        }
        int middle = sorted.size() / 2;
        double seconds = sorted.size() % 2 == 1
                ? sorted.get(middle).toSeconds()
                : (sorted.get(middle - 1).toSeconds() + sorted.get(middle).toSeconds()) / 2.0;
        return round1(seconds / 3600.0);
    }

    private static List<Duration> durations(List<Ticket> tickets, Function<Ticket, Optional<Duration>> metric) {
        return tickets.stream().map(metric).flatMap(Optional::stream).toList();
    }

    private static Double percentage(List<Ticket> tickets, Predicate<Ticket> condition) {
        if (tickets.isEmpty()) {
            return null;
        }
        return round1(100.0 * tickets.stream().filter(condition).count() / tickets.size());
    }

    private static double hours(Duration duration) {
        return round1(duration.toSeconds() / 3600.0);
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
