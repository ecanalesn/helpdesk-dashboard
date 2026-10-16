package io.github.ecanalesn.helpdesk.ticket;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** En la web se muestra como página 404; en la API, como ProblemDetail 404. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(long id) {
        super("No existe el ticket #" + id);
    }
}
