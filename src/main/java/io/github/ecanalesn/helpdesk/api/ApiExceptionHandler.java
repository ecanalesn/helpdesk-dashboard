package io.github.ecanalesn.helpdesk.api;

import io.github.ecanalesn.helpdesk.ticket.TicketNotFoundException;
import io.github.ecanalesn.helpdesk.ticket.TicketStateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Errores de la API en formato estándar RFC 9457 ({@code application/problem+json}).
 * Solo se aplica a los controladores de este paquete: la web sigue mostrando páginas de error HTML.
 * Al extender {@link ResponseEntityExceptionHandler}, los errores de validación (400) salen con el mismo formato.
 */
@RestControllerAdvice(basePackageClasses = ApiExceptionHandler.class)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(TicketNotFoundException.class)
    ProblemDetail notFound(TicketNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Ticket no encontrado", e.getMessage());
    }

    @ExceptionHandler(TicketStateException.class)
    ProblemDetail invalidTransition(TicketStateException e) {
        return problem(HttpStatus.CONFLICT, "Cambio de estado no permitido", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "Petición no válida", e.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
