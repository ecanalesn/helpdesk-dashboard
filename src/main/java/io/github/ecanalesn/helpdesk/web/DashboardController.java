package io.github.ecanalesn.helpdesk.web;

import io.github.ecanalesn.helpdesk.metrics.DashboardMetrics;
import io.github.ecanalesn.helpdesk.metrics.MetricsService;
import io.github.ecanalesn.helpdesk.metrics.MonthlyReportCsv;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.YearMonth;

@Controller
public class DashboardController {

    private final MetricsService metrics;

    public DashboardController(MetricsService metrics) {
        this.metrics = metrics;
    }

    /** Panel de indicadores del mes elegido (por defecto, el actual). Las gráficas piden los datos a /api/metrics. */
    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) YearMonth month, Model model) {
        YearMonth current = metrics.currentMonth();
        YearMonth selected = month != null ? month : current;
        model.addAttribute("metrics", metrics.forMonth(selected));
        model.addAttribute("month", selected);
        model.addAttribute("previousMonth", selected.minusMonths(1));
        model.addAttribute("nextMonth", selected.isBefore(current) ? selected.plusMonths(1) : null);
        return "dashboard";
    }

    /** Informe mensual para dirección, descargable como CSV. */
    @GetMapping("/reports/monthly.csv")
    public ResponseEntity<byte[]> monthlyReport(@RequestParam(required = false) YearMonth month) {
        DashboardMetrics report = metrics.forMonth(month != null ? month : metrics.currentMonth());
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(MonthlyReportCsv.fileName(report)).build().toString())
                .body(MonthlyReportCsv.toBytes(report));
    }
}
