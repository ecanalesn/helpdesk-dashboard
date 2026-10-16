package io.github.ecanalesn.helpdesk.api;

import io.github.ecanalesn.helpdesk.ticket.Priority;
import io.github.ecanalesn.helpdesk.ticket.Status;
import io.github.ecanalesn.helpdesk.ticket.Ticket;
import io.github.ecanalesn.helpdesk.ticket.TicketForm;
import io.github.ecanalesn.helpdesk.ticket.TicketService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

/**
 * API REST de tickets.
 * <pre>
 * GET  /api/tickets?status=&amp;priority=&amp;q=&amp;page=&amp;size=
 * GET  /api/tickets/{id}
 * POST /api/tickets                 → 201 Created + Location
 * POST /api/tickets/{id}/start      {"assignee": "…"}
 * POST /api/tickets/{id}/resolve
 * POST /api/tickets/{id}/reopen
 * </pre>
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketApiController {

    private static final int MAX_PAGE_SIZE = 100;

    private final TicketService service;

    public TicketApiController(TicketService service) {
        this.service = service;
    }

    public record TicketPage(List<TicketResponse> content, int page, int size, long totalElements, int totalPages) {
    }

    public record StartRequest(@NotBlank @Size(max = Ticket.PERSON_MAX) String assignee) {
    }

    @GetMapping
    public TicketPage list(@RequestParam(required = false) Status status,
                           @RequestParam(required = false) Priority priority,
                           @RequestParam(required = false) String q,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size) {
        Page<Ticket> result = service.search(status, priority, q, page, Math.clamp(size, 1, MAX_PAGE_SIZE));
        return new TicketPage(result.map(TicketResponse::from).getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable long id) {
        return TicketResponse.from(service.get(id));
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody TicketForm form) {
        Ticket ticket = service.create(form);
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(ticket.getId()))
                .body(TicketResponse.from(ticket));
    }

    @PostMapping("/{id}/start")
    public TicketResponse start(@PathVariable long id, @Valid @RequestBody StartRequest request) {
        return TicketResponse.from(service.start(id, request.assignee()));
    }

    @PostMapping("/{id}/resolve")
    public TicketResponse resolve(@PathVariable long id) {
        return TicketResponse.from(service.resolve(id));
    }

    @PostMapping("/{id}/reopen")
    public TicketResponse reopen(@PathVariable long id) {
        return TicketResponse.from(service.reopen(id));
    }
}
