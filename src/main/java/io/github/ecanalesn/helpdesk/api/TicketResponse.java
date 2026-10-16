package io.github.ecanalesn.helpdesk.api;

import io.github.ecanalesn.helpdesk.ticket.Category;
import io.github.ecanalesn.helpdesk.ticket.Priority;
import io.github.ecanalesn.helpdesk.ticket.Status;
import io.github.ecanalesn.helpdesk.ticket.Ticket;

import java.time.Duration;
import java.time.Instant;

/** Representación JSON de un ticket. La entidad JPA nunca se expone directamente. */
public record TicketResponse(
        long id,
        String title,
        String description,
        String requester,
        Category category,
        Priority priority,
        Status status,
        String assignee,
        Instant createdAt,
        Instant firstResponseAt,
        Instant resolvedAt,
        Long responseMinutes,
        Long resolutionMinutes,
        Instant responseDeadline,
        Instant resolutionDeadline) {

    public static TicketResponse from(Ticket t) {
        return new TicketResponse(t.getId(), t.getTitle(), t.getDescription(), t.getRequester(), t.getCategory(),
                t.getPriority(), t.getStatus(), t.getAssignee(), t.getCreatedAt(), t.getFirstResponseAt(),
                t.getResolvedAt(), t.responseTime().map(Duration::toMinutes).orElse(null),
                t.resolutionTime().map(Duration::toMinutes).orElse(null), t.responseDeadline(),
                t.resolutionDeadline());
    }
}
