package io.github.ecanalesn.helpdesk.metrics;

import io.github.ecanalesn.helpdesk.ticket.Category;
import io.github.ecanalesn.helpdesk.ticket.Priority;
import io.github.ecanalesn.helpdesk.ticket.Ticket;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonthlyReportCsvTest {

    private static final Instant CREATED = Instant.parse("2026-10-05T08:00:00Z");

    private final DashboardMetrics metrics = metrics();

    @Test
    void writesSummaryAndPriorityTableWithSpanishFormat() {
        List<String> lines = MonthlyReportCsv.render(metrics).lines().toList();

        assertEquals("Informe mensual de soporte;2026-10", lines.get(0));
        assertTrue(lines.contains("Tickets creados;2"));
        assertTrue(lines.contains("Tickets resueltos;1"));
        assertTrue(lines.contains("Tiempo medio de resolución (h);2,5"));
        assertTrue(lines.contains("Cumplimiento SLA de resolución (%);100,0"));
        assertTrue(lines.contains("Media;1;1;0,5;8,0;2,5;72,0;100,0;100,0"));
        // sin datos → celda vacía, no "null" ni 0
        assertTrue(lines.contains("Baja;1;0;;24,0;;120,0;;"));
    }

    @Test
    void startsWithUtf8BomSoExcelReadsTheAccents() {
        byte[] bytes = MonthlyReportCsv.toBytes(metrics);

        assertArrayEquals(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, Arrays.copyOf(bytes, 3));
        assertTrue(new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8).contains("resolución"));
    }

    @Test
    void fileNameIncludesTheMonth() {
        assertEquals("informe-helpdesk-2026-10.csv", MonthlyReportCsv.fileName(metrics));
    }

    private static DashboardMetrics metrics() {
        Ticket resolved = new Ticket("Maven no descarga dependencias", "", "Ana", Category.BUILD, Priority.MEDIA, CREATED);
        resolved.start("Técnico", CREATED.plus(Duration.ofMinutes(30)));
        resolved.resolve(CREATED.plus(Duration.ofMinutes(150)));
        Ticket open = new Ticket("Licencia de IntelliJ", "", "Luis", Category.ACCESOS, Priority.BAJA, CREATED);

        return new MetricsCalculator(ZoneId.of("Europe/Madrid"))
                .calculate(List.of(resolved, open), YearMonth.of(2026, 10), Instant.parse("2026-10-16T08:00:00Z"));
    }
}
