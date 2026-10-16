package io.github.ecanalesn.helpdesk.ticket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketTest {

    private static final Instant CREATED = Instant.parse("2026-10-01T08:00:00Z");

    private static Ticket ticket(Priority priority) {
        return new Ticket("IntelliJ no resuelve dependencias", "  Tras actualizar el pom  ", "Ana", Category.IDE,
                priority, CREATED);
    }

    private static Instant plusHours(double hours) {
        return CREATED.plus(Duration.ofMinutes((long) (hours * 60)));
    }

    @Test
    void newTicketIsOpenWithoutResponseOrResolution() {
        Ticket t = ticket(Priority.MEDIA);

        assertEquals(Status.ABIERTO, t.getStatus());
        assertEquals("Tras actualizar el pom", t.getDescription());
        assertEquals(Optional.empty(), t.responseTime());
        assertEquals(Optional.empty(), t.resolutionTime());
        assertTrue(t.canStart());
        assertTrue(t.canResolve());
        assertFalse(t.canReopen());
    }

    @Test
    void startRegistersFirstResponseAndAssignee() {
        Ticket t = ticket(Priority.MEDIA);

        t.start(" Estefanía ", plusHours(2));

        assertEquals(Status.EN_CURSO, t.getStatus());
        assertEquals("Estefanía", t.getAssignee());
        assertEquals(Optional.of(Duration.ofHours(2)), t.responseTime());
    }

    @Test
    void resolveAfterStartKeepsTheFirstResponse() {
        Ticket t = ticket(Priority.MEDIA);
        t.start("Estefanía", plusHours(2));

        t.resolve(plusHours(5));

        assertEquals(Status.RESUELTO, t.getStatus());
        assertEquals(Optional.of(Duration.ofHours(2)), t.responseTime());
        assertEquals(Optional.of(Duration.ofHours(5)), t.resolutionTime());
    }

    @Test
    void resolvingDirectlyCountsAsFirstResponse() {
        Ticket t = ticket(Priority.BAJA);

        t.resolve(plusHours(1));

        assertEquals(Optional.of(Duration.ofHours(1)), t.responseTime());
        assertEquals(Optional.of(Duration.ofHours(1)), t.resolutionTime());
    }

    @Test
    void reopenClearsResolutionButKeepsFirstResponse() {
        Ticket t = ticket(Priority.MEDIA);
        t.start("Estefanía", plusHours(1));
        t.resolve(plusHours(3));

        t.reopen();

        assertEquals(Status.EN_CURSO, t.getStatus());
        assertNull(t.getResolvedAt());
        assertEquals(Optional.of(Duration.ofHours(1)), t.responseTime());
        assertTrue(t.canResolve());
    }

    @Test
    void rejectsTransitionsThatAreNotAllowed() {
        Ticket t = ticket(Priority.MEDIA);
        assertThrows(TicketStateException.class, t::reopen);

        t.start("Estefanía", plusHours(1));
        assertThrows(TicketStateException.class, () -> t.start("Otro", plusHours(2)));

        t.resolve(plusHours(2));
        assertThrows(TicketStateException.class, () -> t.resolve(plusHours(3)));
        assertThrows(TicketStateException.class, () -> t.start("Otro", plusHours(3)));
    }

    @Test
    void rejectsTimesBeforeThePreviousStep() {
        Ticket t = ticket(Priority.MEDIA);
        assertThrows(IllegalArgumentException.class, () -> t.start("Estefanía", plusHours(-1)));

        t.start("Estefanía", plusHours(2));
        assertThrows(IllegalArgumentException.class, () -> t.resolve(plusHours(1)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void requiresTitleRequesterAndAssignee(String blank) {
        assertThrows(IllegalArgumentException.class,
                () -> new Ticket(blank, "", "Ana", Category.IDE, Priority.MEDIA, CREATED));
        assertThrows(IllegalArgumentException.class,
                () -> new Ticket("Título", "", blank, Category.IDE, Priority.MEDIA, CREATED));
        assertThrows(IllegalArgumentException.class, () -> ticket(Priority.MEDIA).start(blank, plusHours(1)));
    }

    @Test
    void slaUsesTheTargetsOfThePriority() {
        // ALTA: respuesta en 4 h, resolución en 24 h
        Ticket onTime = ticket(Priority.ALTA);
        onTime.start("Estefanía", plusHours(4));
        onTime.resolve(plusHours(24));
        assertTrue(onTime.responseSlaMet(plusHours(100)));
        assertTrue(onTime.resolutionSlaMet(plusHours(100)));

        Ticket late = ticket(Priority.ALTA);
        late.start("Estefanía", plusHours(4.5));
        late.resolve(plusHours(25));
        assertFalse(late.responseSlaMet(plusHours(100)));
        assertFalse(late.resolutionSlaMet(plusHours(100)));
    }

    @Test
    void pendingTicketBreachesSlaOnceTheDeadlinePasses() {
        Ticket t = ticket(Priority.CRITICA); // respuesta en 1 h, resolución en 4 h

        assertEquals(plusHours(1), t.responseDeadline());
        assertEquals(plusHours(4), t.resolutionDeadline());
        assertTrue(t.responseSlaMet(plusHours(0.5)));
        assertFalse(t.responseSlaMet(plusHours(1.5)));
        assertTrue(t.resolutionSlaMet(plusHours(3)));
        assertFalse(t.resolutionSlaMet(plusHours(5)));
    }
}
