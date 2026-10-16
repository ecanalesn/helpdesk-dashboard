package io.github.ecanalesn.helpdesk.ticket;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /** Búsqueda del listado: todos los filtros son opcionales ({@code null} = sin filtrar). */
    @Query("""
            select t from Ticket t
            where (:status is null or t.status = :status)
              and (:priority is null or t.priority = :priority)
              and (:text is null
                   or lower(t.title) like lower(concat('%', :text, '%'))
                   or lower(t.requester) like lower(concat('%', :text, '%')))
            """)
    Page<Ticket> search(@Param("status") Status status,
                        @Param("priority") Priority priority,
                        @Param("text") String text,
                        Pageable pageable);

    /**
     * Tickets con actividad en [from, to): creados antes del final del periodo y que seguían
     * sin resolver al empezarlo. Incluye los creados, respondidos y resueltos en el periodo
     * y los pendientes que vienen de periodos anteriores.
     */
    @Query("select t from Ticket t where t.createdAt < :to and (t.resolvedAt is null or t.resolvedAt >= :from)")
    List<Ticket> findActiveBetween(@Param("from") Instant from, @Param("to") Instant to);
}
