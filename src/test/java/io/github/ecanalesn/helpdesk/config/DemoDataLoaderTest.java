package io.github.ecanalesn.helpdesk.config;

import io.github.ecanalesn.helpdesk.TestClockConfig;
import io.github.ecanalesn.helpdesk.ticket.Status;
import io.github.ecanalesn.helpdesk.ticket.Ticket;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoDataLoaderTest {

    private static final Instant NOW = TestClockConfig.NOW;
    private static final HelpdeskProperties.DemoData CONFIG = new HelpdeskProperties.DemoData(true, 180, 90, 42);

    private final DemoDataLoader loader = new DemoDataLoader(null,
            new HelpdeskProperties(TestClockConfig.ZONE, CONFIG), Clock.fixed(NOW, TestClockConfig.ZONE));

    @Test
    void generatesTheSameDataWithTheSameSeed() {
        List<Ticket> first = loader.generate(CONFIG, NOW);
        List<Ticket> second = loader.generate(CONFIG, NOW);

        assertEquals(180, first.size());
        assertEquals(first.stream().map(Ticket::getCreatedAt).toList(), second.stream().map(Ticket::getCreatedAt).toList());
        assertEquals(first.stream().map(Ticket::getTitle).toList(), second.stream().map(Ticket::getTitle).toList());
    }

    @Test
    void generatesConsistentTicketsInThePast() {
        List<Ticket> tickets = loader.generate(CONFIG, NOW);
        Instant oldest = NOW.minus(Duration.ofDays(CONFIG.days()));

        for (Ticket t : tickets) {
            assertTrue(t.getCreatedAt().isBefore(NOW) && t.getCreatedAt().isAfter(oldest), "fecha fuera de rango");
            if (t.getFirstResponseAt() != null) {
                assertTrue(!t.getFirstResponseAt().isBefore(t.getCreatedAt()) && t.getFirstResponseAt().isBefore(NOW));
            }
            if (t.getResolvedAt() != null) {
                assertTrue(t.getResolvedAt().isAfter(t.getFirstResponseAt()) && t.getResolvedAt().isBefore(NOW));
            }
        }
        Set<Status> statuses = tickets.stream().map(Ticket::getStatus).collect(Collectors.toSet());
        assertEquals(EnumSet.allOf(Status.class), statuses, "debe haber tickets en todos los estados");
    }
}
