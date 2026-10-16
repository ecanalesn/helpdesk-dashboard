package io.github.ecanalesn.helpdesk.web;

import io.github.ecanalesn.helpdesk.ticket.Ticket;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;

/** Formato de fechas, duraciones y cifras para las plantillas: {@code ${@fmt.dateTime(t.createdAt)}}. */
@Component("fmt")
public class ViewFormatter {

    private static final Locale SPANISH = Locale.forLanguageTag("es-ES");
    private static final String EMPTY = "—";

    private final Clock clock;
    private final DateTimeFormatter dateTime;

    public ViewFormatter(Clock clock) {
        this.clock = clock;
        this.dateTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(clock.getZone());
    }

    public String dateTime(Instant instant) {
        return instant == null ? EMPTY : dateTime.format(instant);
    }

    /** "45 min", "3 h 20 min", "2 d 5 h". */
    public String duration(Optional<Duration> duration) {
        return duration.map(this::duration).orElse(EMPTY);
    }

    public String duration(Duration duration) {
        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + " min";
        }
        if (minutes < 24 * 60) {
            return minutes / 60 + " h" + (minutes % 60 == 0 ? "" : " " + minutes % 60 + " min");
        }
        long hours = minutes / 60;
        return hours / 24 + " d" + (hours % 24 == 0 ? "" : " " + hours % 24 + " h");
    }

    /** 2013,7 horas → "83 d 21 h". */
    public String durationFromHours(double hours) {
        return duration(Duration.ofMinutes(Math.round(hours * 60)));
    }

    /** Horas con un decimal y coma decimal: "4,5 h". */
    public String hours(Double hours) {
        return hours == null ? EMPTY : String.format(SPANISH, "%.1f h", hours);
    }

    public String percent(Double percent) {
        return percent == null ? EMPTY : String.format(SPANISH, "%.1f %%", percent);
    }

    public String month(YearMonth month) {
        String name = month.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, SPANISH);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1) + " " + month.getYear();
    }

    /** Antigüedad de un ticket pendiente, o duración total si está resuelto. */
    public String age(Ticket ticket) {
        Instant end = ticket.getResolvedAt() != null ? ticket.getResolvedAt() : clock.instant();
        return duration(Duration.between(ticket.getCreatedAt(), end));
    }

    public boolean resolutionSlaMet(Ticket ticket) {
        return ticket.resolutionSlaMet(clock.instant());
    }

    public boolean responseSlaMet(Ticket ticket) {
        return ticket.responseSlaMet(clock.instant());
    }
}
