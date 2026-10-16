package io.github.ecanalesn.helpdesk.metrics;

import io.github.ecanalesn.helpdesk.ticket.Category;
import io.github.ecanalesn.helpdesk.ticket.Priority;
import io.github.ecanalesn.helpdesk.ticket.Status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Indicadores de un mes. Los tiempos van en horas con un decimal y los porcentajes de 0 a 100;
 * son {@code null} cuando no hay datos (por ejemplo, ningún ticket resuelto en el mes).
 */
public record DashboardMetrics(
        YearMonth month,
        Summary summary,
        List<PriorityMetrics> byPriority,
        List<CategoryCount> byCategory,
        List<DailyCount> daily,
        List<AgingTicket> oldestOpen) {

    /**
     * @param backlog        pendientes al cierre del periodo (o ahora, si es el mes en curso)
     * @param overdueBacklog pendientes que ya han superado su objetivo de resolución
     */
    public record Summary(
            int created,
            int resolved,
            int backlog,
            int open,
            int inProgress,
            int overdueBacklog,
            Double avgResponseHours,
            Double medianResponseHours,
            Double avgResolutionHours,
            Double medianResolutionHours,
            Double responseSlaPct,
            Double resolutionSlaPct) {
    }

    public record PriorityMetrics(
            Priority priority,
            String label,
            int created,
            int resolved,
            Double avgResponseHours,
            Double avgResolutionHours,
            Double responseSlaPct,
            Double resolutionSlaPct,
            double responseTargetHours,
            double resolutionTargetHours) {
    }

    public record CategoryCount(Category category, String label, int created) {
    }

    public record DailyCount(LocalDate date, int created, int resolved) {
    }

    public record AgingTicket(long id, String title, Priority priority, Status status, Instant createdAt,
                              double ageHours, boolean overdue) {
    }
}
