package io.github.ecanalesn.helpdesk.web;

import io.github.ecanalesn.helpdesk.TestClockConfig;
import io.github.ecanalesn.helpdesk.ticket.Category;
import io.github.ecanalesn.helpdesk.ticket.Priority;
import io.github.ecanalesn.helpdesk.ticket.Ticket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewFormatterTest {

    private final ViewFormatter fmt = new ViewFormatter(Clock.fixed(TestClockConfig.NOW, TestClockConfig.ZONE));

    @ParameterizedTest(name = "{0} min → {1}")
    @CsvSource({
            "0,     0 min",
            "45,    45 min",
            "60,    1 h",
            "200,   3 h 20 min",
            "1440,  1 d",
            "3180,  2 d 5 h",
    })
    void formatsDurations(long minutes, String expected) {
        assertEquals(expected, fmt.duration(Duration.ofMinutes(minutes)));
    }

    @Test
    void formatsMissingValuesAsDash() {
        assertEquals("—", fmt.duration(Optional.empty()));
        assertEquals("—", fmt.hours(null));
        assertEquals("—", fmt.percent(null));
        assertEquals("—", fmt.dateTime(null));
    }

    @Test
    void usesSpanishFormatAndConfiguredTimeZone() {
        assertEquals("4,5 h", fmt.hours(4.5));
        assertEquals("83 d 21 h", fmt.durationFromHours(2013.7));
        assertEquals("66,7 %", fmt.percent(66.7));
        assertEquals("16/10/2026 10:00", fmt.dateTime(TestClockConfig.NOW));
        assertEquals("Octubre 2026", fmt.month(YearMonth.of(2026, 10)));
    }

    @Test
    void ageAndSlaUseTheInjectedClock() {
        Ticket ticket = new Ticket("Título", "", "Ana", Category.IDE, Priority.CRITICA,
                Instant.parse("2026-10-16T05:00:00Z"));

        // CRÍTICA: 3 h sin respuesta → respuesta fuera de plazo (1 h), resolución aún en plazo (4 h)
        assertEquals("3 h", fmt.age(ticket));
        assertFalse(fmt.responseSlaMet(ticket));
        assertTrue(fmt.resolutionSlaMet(ticket));
    }
}
