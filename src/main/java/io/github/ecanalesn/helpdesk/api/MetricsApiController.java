package io.github.ecanalesn.helpdesk.api;

import io.github.ecanalesn.helpdesk.metrics.DashboardMetrics;
import io.github.ecanalesn.helpdesk.metrics.MetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

/** Indicadores de un mes en JSON: los consume el panel (Chart.js) y cualquier otra herramienta. */
@RestController
public class MetricsApiController {

    private final MetricsService metrics;

    public MetricsApiController(MetricsService metrics) {
        this.metrics = metrics;
    }

    /** {@code GET /api/metrics?month=2026-10}; sin parámetro, el mes en curso. */
    @GetMapping("/api/metrics")
    public DashboardMetrics metrics(@RequestParam(required = false) YearMonth month) {
        return metrics.forMonth(month != null ? month : metrics.currentMonth());
    }
}
