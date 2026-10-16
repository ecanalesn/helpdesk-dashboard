package io.github.ecanalesn.helpdesk.web;

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
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Pantallas de tickets con las plantillas Thymeleaf reales y el servicio simulado. */
@WebMvcTest(TicketController.class)
@Import({ViewFormatter.class, TestClockConfig.class})
class TicketControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TicketService service;

    @Test
    void listsTickets() throws Exception {
        given(service.search(isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .willReturn(new PageImpl<>(List.of(ticket(7L)), PageRequest.of(0, 20), 1));

        mvc.perform(get("/tickets"))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/list"))
                .andExpect(content().string(containsString("VS Code no detecta el JDK")))
                .andExpect(content().string(containsString("1 tickets")));
    }

    @Test
    void showsTheNewTicketForm() throws Exception {
        mvc.perform(get("/tickets/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Crear ticket")));
    }

    @Test
    void redisplaysTheFormWithValidationErrors() throws Exception {
        mvc.perform(post("/tickets").param("title", "").param("requester", "Ana").param("priority", "ALTA"))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/form"))
                .andExpect(model().attributeHasFieldErrors("form", "title", "category"))
                .andExpect(content().string(containsString("Indica un título")));
        verifyNoInteractions(service);
    }

    @Test
    void createsAndRedirectsToTheDetail() throws Exception {
        given(service.create(any())).willReturn(ticket(7L));

        mvc.perform(post("/tickets").param("title", "VS Code no detecta el JDK").param("requester", "Ana")
                        .param("category", "IDE").param("priority", "ALTA"))
                .andExpect(redirectedUrl("/tickets/7"))
                .andExpect(flash().attribute("success", "Ticket #7 creado"));
    }

    @Test
    void showsTheDetailWithTheAvailableActions() throws Exception {
        given(service.get(7L)).willReturn(ticket(7L));

        mvc.perform(get("/tickets/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Empezar (pasar a en curso)")))
                .andExpect(content().string(containsString("Marcar como resuelto")))
                .andExpect(content().string(containsString("16/10/2026 14:00"))); // creado 10:00 Madrid + 4 h (ALTA)
    }

    @Test
    void invalidTransitionShowsAnErrorMessage() throws Exception {
        given(service.resolve(7L)).willThrow(new TicketStateException("El ticket #7 ya está resuelto"));

        mvc.perform(post("/tickets/7/resolve"))
                .andExpect(redirectedUrl("/tickets/7"))
                .andExpect(flash().attribute("error", "El ticket #7 ya está resuelto"));
    }

    @Test
    void unknownTicketIs404() throws Exception {
        given(service.get(99L)).willThrow(new TicketNotFoundException(99L));

        mvc.perform(get("/tickets/99")).andExpect(status().isNotFound());
    }

    private static Ticket ticket(long id) {
        Ticket ticket = new Ticket("VS Code no detecta el JDK", "", "Ana", Category.IDE, Priority.ALTA, TestClockConfig.NOW);
        ReflectionTestUtils.setField(ticket, "id", id);
        return ticket;
    }
}
