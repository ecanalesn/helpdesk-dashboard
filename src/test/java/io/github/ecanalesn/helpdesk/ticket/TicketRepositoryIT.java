package io.github.ecanalesn.helpdesk.ticket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Consultas JPQL del repositorio contra una base de datos H2 real (cada test hace rollback). */
@DataJpaTest
class TicketRepositoryIT {

    private static final Instant OCT_1 = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant NOV_1 = Instant.parse("2026-11-01T00:00:00Z");
    private static final PageRequest FIRST_PAGE = PageRequest.of(0, 20, Sort.by("title"));

    @Autowired
    private TicketRepository repository;

    @BeforeEach
    void setUp() {
        repository.saveAll(List.of(
                // creado y resuelto antes de octubre → sin actividad en octubre
                resolved("Antiguo resuelto", Priority.BAJA, "Ana", OCT_1.minus(days(20)), OCT_1.minus(days(10))),
                // creado en septiembre, resuelto en octubre
                resolved("Resuelto en octubre", Priority.MEDIA, "Luis", OCT_1.minus(days(5)), OCT_1.plus(days(2))),
                // creado en septiembre, sigue abierto
                open("Pendiente de septiembre", Priority.ALTA, "Marta Gil", OCT_1.minus(days(3))),
                // creado en octubre
                open("IntelliJ no arranca", Priority.ALTA, "Sergio", OCT_1.plus(days(4))),
                // creado en noviembre → fuera del periodo
                open("Creado en noviembre", Priority.CRITICA, "Paula", NOV_1.plus(days(1)))));
    }

    @Test
    void findsTicketsWithActivityInThePeriod() {
        List<String> titles = repository.findActiveBetween(OCT_1, NOV_1).stream().map(Ticket::getTitle).sorted().toList();

        assertEquals(List.of("IntelliJ no arranca", "Pendiente de septiembre", "Resuelto en octubre"), titles);
    }

    @Test
    void searchesWithoutFilters() {
        assertEquals(5, repository.search(null, null, null, FIRST_PAGE).getTotalElements());
    }

    @Test
    void filtersByStatusAndPriority() {
        Page<Ticket> page = repository.search(Status.ABIERTO, Priority.ALTA, null, FIRST_PAGE);

        assertEquals(List.of("IntelliJ no arranca", "Pendiente de septiembre"), titles(page));
    }

    @Test
    void searchesTitleAndRequesterIgnoringCase() {
        assertEquals(List.of("IntelliJ no arranca"), titles(repository.search(null, null, "intellij", FIRST_PAGE)));
        assertEquals(List.of("Pendiente de septiembre"), titles(repository.search(null, null, "GIL", FIRST_PAGE)));
    }

    @Test
    void paginatesResults() {
        Page<Ticket> page = repository.search(null, null, null, PageRequest.of(1, 2, Sort.by("title")));

        assertEquals(3, page.getTotalPages());
        assertEquals(List.of("IntelliJ no arranca", "Pendiente de septiembre"), titles(page));
    }

    private static List<String> titles(Page<Ticket> page) {
        return page.getContent().stream().map(Ticket::getTitle).toList();
    }

    private static Ticket open(String title, Priority priority, String requester, Instant created) {
        return new Ticket(title, "", requester, Category.IDE, priority, created);
    }

    private static Ticket resolved(String title, Priority priority, String requester, Instant created, Instant resolvedAt) {
        Ticket ticket = open(title, priority, requester, created);
        ticket.start("Técnico", created.plus(Duration.ofHours(1)));
        ticket.resolve(resolvedAt);
        return ticket;
    }

    private static Duration days(int days) {
        return Duration.ofDays(days);
    }
}
