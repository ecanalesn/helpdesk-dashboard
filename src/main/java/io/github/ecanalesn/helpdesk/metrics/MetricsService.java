package io.github.ecanalesn.helpdesk.metrics;

import io.github.ecanalesn.helpdesk.ticket.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.YearMonth;

@Service
@Transactional(readOnly = true)
public class MetricsService {

    private final TicketRepository repository;
    private final Clock clock;
    private final MetricsCalculator calculator;

    public MetricsService(TicketRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
        this.calculator = new MetricsCalculator(clock.getZone());
    }

    public DashboardMetrics forMonth(YearMonth month) {
        return calculator.calculate(
                repository.findActiveBetween(calculator.start(month), calculator.end(month)),
                month,
                clock.instant());
    }

    public YearMonth currentMonth() {
        return YearMonth.now(clock);
    }
}
