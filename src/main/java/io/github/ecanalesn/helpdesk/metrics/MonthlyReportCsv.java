package io.github.ecanalesn.helpdesk.metrics;

import io.github.ecanalesn.helpdesk.metrics.DashboardMetrics.PriorityMetrics;
import io.github.ecanalesn.helpdesk.metrics.DashboardMetrics.Summary;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Informe mensual para dirección en CSV, pensado para abrirse con doble clic en Excel en español:
 * separador punto y coma, coma decimal y BOM de UTF-8 (sin él, Excel estropea las tildes).
 */
public final class MonthlyReportCsv {

    private static final char SEPARATOR = ';';
    private static final Locale SPANISH = Locale.forLanguageTag("es-ES");
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private MonthlyReportCsv() {
    }

    public static String fileName(DashboardMetrics metrics) {
        return "informe-helpdesk-" + metrics.month() + ".csv";
    }

    public static byte[] toBytes(DashboardMetrics metrics) {
        byte[] content = render(metrics).getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[UTF8_BOM.length + content.length];
        System.arraycopy(UTF8_BOM, 0, result, 0, UTF8_BOM.length);
        System.arraycopy(content, 0, result, UTF8_BOM.length, content.length);
        return result;
    }

    public static String render(DashboardMetrics metrics) {
        Summary s = metrics.summary();
        StringBuilder csv = new StringBuilder();
        row(csv, "Informe mensual de soporte", metrics.month().toString());
        csv.append('\n');
        row(csv, "Indicador", "Valor");
        row(csv, "Tickets creados", s.created());
        row(csv, "Tickets resueltos", s.resolved());
        row(csv, "Pendientes al cierre", s.backlog());
        row(csv, "Pendientes fuera de SLA", s.overdueBacklog());
        row(csv, "Tiempo medio de primera respuesta (h)", s.avgResponseHours());
        row(csv, "Mediana de primera respuesta (h)", s.medianResponseHours());
        row(csv, "Tiempo medio de resolución (h)", s.avgResolutionHours());
        row(csv, "Mediana de resolución (h)", s.medianResolutionHours());
        row(csv, "Cumplimiento SLA de respuesta (%)", s.responseSlaPct());
        row(csv, "Cumplimiento SLA de resolución (%)", s.resolutionSlaPct());
        csv.append('\n');
        row(csv, "Prioridad", "Creados", "Resueltos", "Respuesta media (h)", "Objetivo respuesta (h)",
                "Resolución media (h)", "Objetivo resolución (h)", "SLA respuesta (%)", "SLA resolución (%)");
        for (PriorityMetrics p : metrics.byPriority()) {
            row(csv, p.label(), p.created(), p.resolved(), p.avgResponseHours(), p.responseTargetHours(),
                    p.avgResolutionHours(), p.resolutionTargetHours(), p.responseSlaPct(), p.resolutionSlaPct());
        }
        return csv.toString();
    }

    private static void row(StringBuilder csv, Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(SEPARATOR);
            }
            csv.append(format(values[i]));
        }
        csv.append('\n');
    }

    private static String format(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Double number) {
            return String.format(SPANISH, "%.1f", number);
        }
        String text = value.toString();
        boolean needsQuotes = text.indexOf(SEPARATOR) >= 0 || text.indexOf('"') >= 0 || text.indexOf('\n') >= 0;
        return needsQuotes ? '"' + text.replace("\"", "\"\"") + '"' : text;
    }
}
