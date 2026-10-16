package io.github.ecanalesn.helpdesk.metrics;

import io.github.ecanalesn.helpdesk.metrics.DashboardMetrics.AgingTicket;
import io.github.ecanalesn.helpdesk.metrics.DashboardMetrics.PriorityMetrics;
import io.github.ecanalesn.helpdesk.metrics.DashboardMetrics.Summary;
import io.github.ecanalesn.helpdesk.ticket.Category;
import io.github.ecanalesn.helpdesk.ticket.Priority;
import io.github.ecanalesn.helpdesk.ticket.Status;
import io.github.ecanalesn.helpdesk.ticket.Ticket;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Escenario fijo (octubre de 2026, "ahora" = 16/10 10:00 en Madrid):
 * <pre>
 * t1 CRÍTICA  creado 01/10 09:00, respuesta +0,5 h, resuelto +3 h     → dentro de SLA
 * t2 ALTA     creado 02/10 10:00, respuesta +6 h,   resuelto +30 h    → fuera de SLA (4 h / 24 h)
 * t3 MEDIA    creado 05/10 12:00, respuesta +2 h,   sin resolver      → pendiente fuera de SLA (72 h)
 * t4 BAJA     creado 15/10 09:00, sin respuesta                       → pendiente en plazo (120 h)
 * t5 MEDIA    creado 20/09 10:00, respuesta +1 h,   resuelto 03/10    → resuelto en octubre, creado en septiembre
 * </pre>
 */
class MetricsCalculatorTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final YearMonth OCTOBER = YearMonth.of(2026, 10);
    private static final Instant NOW = madrid(2026, 10, 16, 10, 0);

    private final MetricsCalculator calculator = new MetricsCalculator(MADRID);

    private final List<Ticket> tickets = List.of(
            ticket("t1", Priority.CRITICA, Category.IDE, madrid(2026, 10, 1, 9, 0), 0.5, 3.0),
            ticket("t2", Priority.ALTA, Category.BUILD, madrid(2026, 10, 2, 10, 0), 6.0, 30.0),
            ticket("t3", Priority.MEDIA, Category.BUILD, madrid(2026, 10, 5, 12, 0), 2.0, null),
            ticket("t4", Priority.BAJA, Category.CONTROL_VERSIONES, madrid(2026, 10, 15, 9, 0), null, null),
            ticket("t5", Priority.MEDIA, Category.IDE, madrid(2026, 9, 20, 10, 0), 1.0, 312.0));

    @Test
    void summarisesTheMonth() {
        Summary s = calculator.calculate(tickets, OCTOBER, NOW).summary();

        assertEquals(4, s.created());
        assertEquals(3, s.resolved());
        assertEquals(2, s.backlog());
        assertEquals(1, s.open());
        assertEquals(1, s.inProgress());
        assertEquals(1, s.overdueBacklog());
        // respuesta: t1 0,5 h · t2 6 h · t3 2 h
        assertEquals(2.8, s.avgResponseHours());
        assertEquals(2.0, s.medianResponseHours());
        assertEquals(66.7, s.responseSlaPct());
        // resolución: t1 3 h · t2 30 h · t5 312 h
        assertEquals(115.0, s.avgResolutionHours());
        assertEquals(30.0, s.medianResolutionHours());
        assertEquals(33.3, s.resolutionSlaPct());
    }

    @Test
    void breaksDownByPriority() {
        List<PriorityMetrics> byPriority = calculator.calculate(tickets, OCTOBER, NOW).byPriority();

        assertEquals(List.of(Priority.CRITICA, Priority.ALTA, Priority.MEDIA, Priority.BAJA),
                byPriority.stream().map(PriorityMetrics::priority).toList());
        assertEquals(new PriorityMetrics(Priority.CRITICA, "Crítica", 1, 1, 0.5, 3.0, 100.0, 100.0, 1.0, 4.0),
                byPriority.get(0));
        assertEquals(new PriorityMetrics(Priority.ALTA, "Alta", 1, 1, 6.0, 30.0, 0.0, 0.0, 4.0, 24.0),
                byPriority.get(1));
        assertEquals(new PriorityMetrics(Priority.MEDIA, "Media", 1, 1, 2.0, 312.0, 100.0, 0.0, 8.0, 72.0),
                byPriority.get(2));
        assertEquals(new PriorityMetrics(Priority.BAJA, "Baja", 1, 0, null, null, null, null, 24.0, 120.0),
                byPriority.get(3));
    }

    @Test
    void countsCreatedTicketsByCategory() {
        var byCategory = calculator.calculate(tickets, OCTOBER, NOW).byCategory();

        assertEquals(Category.values().length, byCategory.size());
        assertEquals(1, find(byCategory, Category.IDE));
        assertEquals(2, find(byCategory, Category.BUILD));
        assertEquals(1, find(byCategory, Category.CONTROL_VERSIONES));
        assertEquals(0, find(byCategory, Category.ACCESOS));
    }

    @Test
    void dailySeriesStopsAtTodayForTheCurrentMonth() {
        var daily = calculator.calculate(tickets, OCTOBER, NOW).daily();

        assertEquals(16, daily.size());
        assertEquals(LocalDate.of(2026, 10, 1), daily.get(0).date());
        assertEquals(1, daily.get(0).created());
        assertEquals(1, daily.get(0).resolved());   // t1
        assertEquals(2, daily.get(2).resolved());   // t2 y t5 el día 3
    }

    @Test
    void listsOldestPendingTicketsFirst() {
        List<AgingTicket> oldest = calculator.calculate(tickets, OCTOBER, NOW).oldestOpen();

        assertEquals(List.of("t3", "t4"), oldest.stream().map(AgingTicket::title).toList());
        assertEquals(Status.EN_CURSO, oldest.get(0).status());
        assertEquals(262.0, oldest.get(0).ageHours());
        assertTrue(oldest.get(0).overdue());
        assertEquals(Status.ABIERTO, oldest.get(1).status());
    }

    @Test
    void pastMonthTakesTheBacklogAtTheEndOfThatMonth() {
        DashboardMetrics september = calculator.calculate(tickets, YearMonth.of(2026, 9), NOW);

        assertEquals(1, september.summary().created());      // t5
        assertEquals(1, september.summary().backlog());      // t5 seguía abierto el 1 de octubre
        assertEquals(1, september.summary().inProgress());
        assertEquals(30, september.daily().size());
    }

    @Test
    void emptyMonthHasNoAveragesInsteadOfZeros() {
        Summary s = calculator.calculate(List.of(), OCTOBER, NOW).summary();

        assertEquals(0, s.created());
        assertNull(s.avgResponseHours());
        assertNull(s.medianResolutionHours());
        assertNull(s.resolutionSlaPct());
    }

    @Test
    void periodBoundariesFollowTheConfiguredTimeZone() {
        // 1 de octubre a las 00:00 en Madrid son las 22:00 UTC del 30 de septiembre
        assertEquals(Instant.parse("2026-09-30T22:00:00Z"), calculator.start(OCTOBER));
        // 1 de noviembre: ya en horario de invierno (UTC+1)
        assertEquals(Instant.parse("2026-10-31T23:00:00Z"), calculator.end(OCTOBER));
    }

    // ------------------------------------------------------------------ utilidades

    private static Ticket ticket(String title, Priority priority, Category category, Instant created,
                                 Double responseHours, Double resolutionHours) {
        Ticket t = new Ticket(title, "", "Solicitante", category, priority, created);
        if (responseHours != null) {
            t.start("Técnico", created.plus(hours(responseHours)));
        }
        if (resolutionHours != null) {
            t.resolve(created.plus(hours(resolutionHours)));
        }
        return t;
    }

    private static Duration hours(double hours) {
        return Duration.ofMinutes(Math.round(hours * 60));
    }

    private static Instant madrid(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(MADRID).toInstant();
    }

    private static int find(List<DashboardMetrics.CategoryCount> counts, Category category) {
        return counts.stream().filter(c -> c.category() == category).findFirst().orElseThrow().created();
    }
}
