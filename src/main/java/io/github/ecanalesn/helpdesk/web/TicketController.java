package io.github.ecanalesn.helpdesk.web;

import io.github.ecanalesn.helpdesk.ticket.Category;
import io.github.ecanalesn.helpdesk.ticket.Priority;
import io.github.ecanalesn.helpdesk.ticket.Status;
import io.github.ecanalesn.helpdesk.ticket.Ticket;
import io.github.ecanalesn.helpdesk.ticket.TicketForm;
import io.github.ecanalesn.helpdesk.ticket.TicketService;
import io.github.ecanalesn.helpdesk.ticket.TicketStateException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.function.Supplier;

/** Pantallas de tickets: listado con filtros, alta y detalle con los cambios de estado. */
@Controller
@RequestMapping("/tickets")
public class TicketController {

    static final int PAGE_SIZE = 20;

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @ModelAttribute
    void enums(Model model) {
        model.addAttribute("statuses", Status.values());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("categories", Category.values());
    }

    @GetMapping
    public String list(@RequestParam(required = false) Status status,
                       @RequestParam(required = false) Priority priority,
                       @RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("page", service.search(status, priority, q, page, PAGE_SIZE));
        model.addAttribute("status", status);
        model.addAttribute("priority", priority);
        model.addAttribute("q", q);
        return "tickets/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new TicketForm());
        return "tickets/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") TicketForm form, BindingResult errors,
                         RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            return "tickets/form";
        }
        Ticket ticket = service.create(form);
        redirect.addFlashAttribute("success", "Ticket #" + ticket.getId() + " creado");
        return "redirect:/tickets/" + ticket.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("ticket", service.get(id));
        return "tickets/detail";
    }

    @PostMapping("/{id}/start")
    public String start(@PathVariable long id, @RequestParam String assignee, RedirectAttributes redirect) {
        return change(id, redirect, "Ticket en curso", () -> service.start(id, assignee));
    }

    @PostMapping("/{id}/resolve")
    public String resolve(@PathVariable long id, RedirectAttributes redirect) {
        return change(id, redirect, "Ticket resuelto", () -> service.resolve(id));
    }

    @PostMapping("/{id}/reopen")
    public String reopen(@PathVariable long id, RedirectAttributes redirect) {
        return change(id, redirect, "Ticket reabierto", () -> service.reopen(id));
    }

    /** Ejecuta el cambio y vuelve al detalle con un mensaje de éxito o de error (patrón Post/Redirect/Get). */
    private static String change(long id, RedirectAttributes redirect, String successMessage, Supplier<Ticket> action) {
        try {
            action.get();
            redirect.addFlashAttribute("success", successMessage);
        } catch (TicketStateException | IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tickets/" + id;
    }
}
