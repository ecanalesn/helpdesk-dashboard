package io.github.ecanalesn.helpdesk.ticket;

/** Se intenta un cambio de estado que no está permitido (por ejemplo, resolver un ticket ya resuelto). */
public class TicketStateException extends RuntimeException {

    public TicketStateException(String message) {
        super(message);
    }
}
