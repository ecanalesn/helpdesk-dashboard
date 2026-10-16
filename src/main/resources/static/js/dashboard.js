// Gráficas del panel. Los datos salen de la API REST: GET /api/metrics?month=AAAA-MM
(function () {
    'use strict';

    const root = document.getElementById('charts');
    if (!root || typeof Chart === 'undefined') {
        return;
    }

    // Colores por rol, definidos en app.css (claro y oscuro): el JS no lleva hex sueltos
    const css = getComputedStyle(root);
    const color = (name) => css.getPropertyValue(name).trim();
    const palette = {
        series1: color('--series-1'),
        series2: color('--series-2'),
        text: color('--text-secondary'),
        muted: color('--text-muted'),
        grid: color('--grid'),
        surface: color('--surface-1')
    };

    const number = new Intl.NumberFormat('es-ES', { maximumFractionDigits: 1 });
    const dayLabel = new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'short' });

    Chart.defaults.font.family = 'system-ui, -apple-system, "Segoe UI", sans-serif';
    Chart.defaults.color = palette.text;
    Chart.defaults.borderColor = palette.grid;
    Chart.defaults.plugins.legend.labels.usePointStyle = true;
    Chart.defaults.plugins.legend.labels.boxHeight = 8;
    Chart.defaults.plugins.tooltip.padding = 10;
    Chart.defaults.maintainAspectRatio = false;

    // Ejes y rejilla discretos: líneas finas y continuas
    const axis = (extra) => Object.assign({
        grid: { color: palette.grid, drawTicks: false },
        border: { display: false },
        ticks: { color: palette.muted, padding: 6 }
    }, extra);

    fetch('/api/metrics?month=' + encodeURIComponent(root.dataset.month), { headers: { Accept: 'application/json' } })
        .then((response) => {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            return response.json();
        })
        .then(render)
        .catch((error) => {
            root.insertAdjacentHTML('afterbegin',
                '<div class="col-12"><div class="alert alert-warning mb-0">No se pudieron cargar las gráficas ('
                + error.message + ').</div></div>');
        });

    function render(metrics) {
        dailyChart(metrics.daily);
        categoryChart(metrics.byCategory);
        slaChart(metrics.byPriority);
    }

    // Evolución diaria: dos series en el mismo eje (misma unidad), tooltip con cruz vertical
    function dailyChart(daily) {
        new Chart(document.getElementById('dailyChart'), {
            type: 'line',
            data: {
                labels: daily.map((d) => dayLabel.format(new Date(d.date + 'T00:00:00'))),
                datasets: [
                    line('Creados', daily.map((d) => d.created), palette.series1),
                    line('Resueltos', daily.map((d) => d.resolved), palette.series2)
                ]
            },
            options: {
                interaction: { mode: 'index', intersect: false },
                plugins: { legend: { position: 'top', align: 'start' } },
                scales: {
                    x: axis({ grid: { display: false }, ticks: { color: palette.muted, maxRotation: 0, autoSkipPadding: 12 } }),
                    y: axis({ beginAtZero: true, ticks: { color: palette.muted, precision: 0 } })
                }
            }
        });
    }

    function line(label, data, colour) {
        return {
            label: label,
            data: data,
            borderColor: colour,
            backgroundColor: colour,
            borderWidth: 2,
            tension: 0,
            pointRadius: 0,
            pointHoverRadius: 5,
            pointHoverBorderColor: palette.surface,
            pointHoverBorderWidth: 2
        };
    }

    // Áreas: una sola serie → un solo color para todas las barras, sin leyenda (el título la nombra)
    function categoryChart(categories) {
        new Chart(document.getElementById('categoryChart'), {
            type: 'bar',
            data: {
                labels: categories.map((c) => c.label),
                datasets: [{
                    label: 'Tickets creados',
                    data: categories.map((c) => c.created),
                    backgroundColor: palette.series1,
                    borderRadius: 4,
                    borderSkipped: 'start',
                    barPercentage: 0.7,
                    categoryPercentage: 0.9
                }]
            },
            options: {
                indexAxis: 'y',
                plugins: { legend: { display: false } },
                scales: {
                    x: axis({ beginAtZero: true, ticks: { color: palette.muted, precision: 0 } }),
                    y: axis({ grid: { display: false } })
                }
            }
        });
    }

    // SLA: barras agrupadas en porcentaje (0–100), respuesta frente a resolución
    function slaChart(priorities) {
        new Chart(document.getElementById('slaChart'), {
            type: 'bar',
            data: {
                labels: priorities.map((p) => p.label),
                datasets: [
                    bar('SLA de respuesta', priorities.map((p) => p.responseSlaPct), palette.series1),
                    bar('SLA de resolución', priorities.map((p) => p.resolutionSlaPct), palette.series2)
                ]
            },
            options: {
                interaction: { mode: 'index', intersect: false },
                plugins: {
                    legend: { position: 'top', align: 'start' },
                    tooltip: {
                        callbacks: {
                            label: (ctx) => ctx.dataset.label + ': '
                                + (ctx.raw === null ? 'sin datos' : number.format(ctx.raw) + ' %')
                        }
                    }
                },
                scales: {
                    x: axis({ grid: { display: false } }),
                    y: axis({ min: 0, max: 100, ticks: { color: palette.muted, callback: (v) => v + ' %' } })
                }
            }
        });
    }

    function bar(label, data, colour) {
        return {
            label: label,
            data: data,
            backgroundColor: colour,
            borderRadius: 4,
            borderSkipped: 'start',
            // Separación de 2px entre barras contiguas, del color de la superficie
            borderColor: palette.surface,
            borderWidth: { left: 1, right: 1 },
            barPercentage: 0.8,
            categoryPercentage: 0.6
        };
    }
})();
