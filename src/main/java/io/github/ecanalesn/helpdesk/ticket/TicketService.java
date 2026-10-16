package io.github.ecanalesn.helpdesk.ticket;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Casos de uso sobre tickets. La hora sale del {@link Clock} inyectado, para poder fijarla en los tests. */
@Service
@Transactional
public class TicketService {

    private final TicketRepository repository;
    private final Clock clock;

    public TicketService(TicketRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Ticket create(TicketForm form) {
        return repository.save(new Ticket(form.getTitle(), form.getDescription(), form.getRequester(),
                form.getCategory(), form.getPriority(), clock.instant()));
    }

    @Transactional(readOnly = true)
    public Ticket get(long id) {
        return repository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    }

    /** Listado paginado, del más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public Page<Ticket> search(Status status, Priority priority, String text, int page, int size) {
        String query = text == null || text.isBlank() ? null : text.strip();
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repository.search(status, priority, query, pageRequest);
    }

    public Ticket start(long id, String assignee) {
        Ticket ticket = get(id);
        ticket.start(assignee, clock.instant());
        return ticket;
    }

    public Ticket resolve(long id) {
        Ticket ticket = get(id);
        ticket.resolve(clock.instant());
        return ticket;
    }

    public Ticket reopen(long id) {
        Ticket ticket = get(id);
        ticket.reopen();
        return ticket;
    }
}
