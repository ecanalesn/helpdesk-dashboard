package io.github.ecanalesn.helpdesk.api;

import io.github.ecanalesn.helpdesk.TestClockConfig;
import io.github.ecanalesn.helpdesk.ticket.Category;
import io.github.ecanalesn.helpdesk.ticket.Priority;
import io.github.ecanalesn.helpdesk.ticket.Ticket;
import io.github.ecanalesn.helpdesk.ticket.TicketNotFoundException;
import io.github.ecanalesn.helpdesk.ticket.TicketService;
import io.github.ecanalesn.helpdesk.ticket.TicketStateException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Test de la capa web de la API: solo arranca MVC, con el servicio simulado. */
@WebMvcTest(TicketApiController.class)
class TicketApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TicketService service;

    @Test
    void createsTicketAndReturnsLocation() throws Exception {
        given(service.create(any())).willReturn(ticket(7L));

        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content("""
                        {"title": "VS Code no detecta el JDK", "requester": "Ana",
                         "category": "IDE", "priority": "ALTA"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/tickets/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("ABIERTO"))
                .andExpect(jsonPath("$.priority").value("ALTA"))
                .andExpect(jsonPath("$.responseDeadline").value("2026-10-16T12:00:00Z"));
    }

    @Test
    void rejectsInvalidTicketWithProblemDetail() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content("""
                        {"title": " ", "requester": "Ana", "category": "IDE"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(service);
    }

    @Test
    void returns404ProblemForUnknownTicket() throws Exception {
        given(service.get(99L)).willThrow(new TicketNotFoundException(99L));

        mvc.perform(get("/api/tickets/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Ticket no encontrado"))
                .andExpect(jsonPath("$.detail").value("No existe el ticket #99"));
    }

    @Test
    void returns409WhenTheTransitionIsNotAllowed() throws Exception {
        given(service.resolve(7L)).willThrow(new TicketStateException("El ticket #7 ya está resuelto"));

        mvc.perform(post("/api/tickets/7/resolve"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El ticket #7 ya está resuelto"));
    }

    @Test
    void startRequiresAssignee() throws Exception {
        mvc.perform(post("/api/tickets/7/start").contentType(MediaType.APPLICATION_JSON).content("{\"assignee\": \"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void startAssignsTheTechnician() throws Exception {
        Ticket started = ticket(7L);
        started.start("Estefanía", TestClockConfig.NOW);
        given(service.start(eq(7L), eq("Estefanía"))).willReturn(started);

        mvc.perform(post("/api/tickets/7/start").contentType(MediaType.APPLICATION_JSON).content("{\"assignee\": \"Estefanía\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_CURSO"))
                .andExpect(jsonPath("$.assignee").value("Estefanía"))
                .andExpect(jsonPath("$.responseMinutes").value(0));
    }

    @Test
    void rejectsUnknownEnumValueInFilters() throws Exception {
        mvc.perform(get("/api/tickets").param("status", "PERDIDO"))
                .andExpect(status().isBadRequest());
    }

    static Ticket ticket(long id) {
        Ticket ticket = new Ticket("VS Code no detecta el JDK", "", "Ana", Category.IDE, Priority.ALTA, TestClockConfig.NOW);
        ReflectionTestUtils.setField(ticket, "id", id);
        return ticket;
    }
}
