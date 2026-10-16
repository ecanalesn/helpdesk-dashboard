package io.github.ecanalesn.helpdesk.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos de alta de un ticket: lo usan el formulario web y la API (POST /api/tickets). */
public class TicketForm {

    @NotBlank(message = "Indica un título")
    @Size(max = Ticket.TITLE_MAX, message = "Máximo {max} caracteres")
    private String title;

    @Size(max = Ticket.DESCRIPTION_MAX, message = "Máximo {max} caracteres")
    private String description;

    @NotBlank(message = "Indica quién lo solicita")
    @Size(max = Ticket.PERSON_MAX, message = "Máximo {max} caracteres")
    private String requester;

    @NotNull(message = "Elige un área")
    private Category category;

    @NotNull(message = "Elige una prioridad")
    private Priority priority = Priority.MEDIA;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }
}
