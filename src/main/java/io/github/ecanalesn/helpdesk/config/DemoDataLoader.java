package io.github.ecanalesn.helpdesk.config;

import io.github.ecanalesn.helpdesk.ticket.Category;
import io.github.ecanalesn.helpdesk.ticket.Priority;
import io.github.ecanalesn.helpdesk.ticket.Ticket;
import io.github.ecanalesn.helpdesk.ticket.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Genera tickets ficticios de los últimos meses para que el panel tenga datos al arrancar.
 * Usa una semilla fija: los mismos datos en cada arranque y en cada equipo.
 * Se activa con {@code helpdesk.demo-data.enabled=true}.
 */
@Component
@ConditionalOnBooleanProperty("helpdesk.demo-data.enabled")
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private static final Map<Category, List<String>> TITLES = Map.of(
            Category.IDE, List.of(
                    "IntelliJ no reconoce las dependencias tras actualizar el pom.xml",
                    "VS Code no detecta el JDK 21",
                    "El depurador no se detiene en los breakpoints",
                    "Eclipse muestra errores de compilación que Maven no da",
                    "Plugin de formato de código incompatible con la nueva versión del IDE"),
            Category.CONTROL_VERSIONES, List.of(
                    "Permiso denegado al hacer push al repositorio",
                    "Conflicto de merge en la rama release",
                    "Recuperar commits tras un reset por error",
                    "Configurar clave SSH para GitHub Enterprise",
                    "El repositorio tarda mucho en clonar por ficheros grandes"),
            Category.BUILD, List.of(
                    "PKIX path building failed al descargar de Maven Central",
                    "Conflicto de versiones de dependencias transitivas",
                    "La build falla en local pero no en CI",
                    "Configurar el mirror corporativo en settings.xml",
                    "Artefacto SNAPSHOT no se actualiza en el repositorio local"),
            Category.INTEGRACION_CONTINUA, List.of(
                    "La pipeline falla en el paso de tests",
                    "El runner se queda sin espacio en disco",
                    "Secretos no disponibles en las pull requests",
                    "La caché de dependencias no se reutiliza entre ejecuciones"),
            Category.ACCESOS, List.of(
                    "Solicitud de licencia de IntelliJ IDEA Ultimate",
                    "Alta en la organización de GitHub para nueva incorporación",
                    "Acceso al repositorio de artefactos",
                    "Token de acceso personal caducado"),
            Category.OTROS, List.of(
                    "Duda sobre el flujo de ramas del equipo",
                    "Petición de documentación de onboarding",
                    "Formación sobre buenas prácticas de commits"));

    private static final List<String> REQUESTERS = List.of(
            "Lucía Martín", "Javier Romero", "Marta Navarro", "Sergio Torres", "Paula Gil",
            "Daniel Ortega", "Elena Rubio", "Hugo Serrano", "Clara Molina", "Adrián Vidal");

    private static final List<String> TECHNICIANS = List.of("Estefanía C.", "Andrés P.", "Nuria L.");

    private final TicketRepository repository;
    private final HelpdeskProperties properties;
    private final Clock clock;

    public DemoDataLoader(TicketRepository repository, HelpdeskProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        List<Ticket> tickets = generate(properties.demoData(), clock.instant());
        repository.saveAll(tickets);
        log.info("Cargados {} tickets de demostración", tickets.size());
    }

    List<Ticket> generate(HelpdeskProperties.DemoData config, Instant now) {
        Random random = new Random(config.seed());
        List<Category> categories = List.of(Category.values());
        List<Ticket> tickets = new ArrayList<>();

        for (int i = 0; i < config.tickets(); i++) {
            Instant created = randomWorkingTime(random, now, config.days());
            Category category = categories.get(random.nextInt(categories.size()));
            List<String> titles = TITLES.get(category);
            Ticket ticket = new Ticket(
                    titles.get(random.nextInt(titles.size())),
                    "Incidencia de ejemplo generada para la demostración.",
                    REQUESTERS.get(random.nextInt(REQUESTERS.size())),
                    category,
                    randomPriority(random),
                    created);

            // Parte de los tickets de los últimos tres días sigue en cola, esperando técnico,
            // y alguno se queda atascado "en curso" (dependencia de otro equipo, falta información…)
            boolean waiting = Duration.between(created, now).toHours() < 72 && random.nextInt(100) < 50;
            boolean stuck = random.nextInt(100) < 3;
            // Tiempos alrededor del objetivo de SLA: la mayoría lo cumple y alguno se pasa
            Instant responded = created.plus(scale(ticket.getPriority().getResponseTarget(), 0.05 + random.nextDouble() * 1.2));
            if (!waiting && responded.isBefore(now)) {
                ticket.start(TECHNICIANS.get(random.nextInt(TECHNICIANS.size())), responded);
                Instant resolved = created.plus(scale(ticket.getPriority().getResolutionTarget(), 0.15 + random.nextDouble() * 1.15));
                if (!stuck && resolved.isBefore(now) && resolved.isAfter(responded)) {
                    ticket.resolve(resolved);
                }
            }
            tickets.add(ticket);
        }
        return tickets;
    }

    /** Un instante de los últimos {@code days} días, en horario laboral (8:00–19:00) de lunes a viernes. */
    private Instant randomWorkingTime(Random random, Instant now, int days) {
        ZonedDateTime today = now.atZone(clock.getZone());
        while (true) {
            ZonedDateTime day = today.minusDays(random.nextInt(days));
            if (day.getDayOfWeek().getValue() >= 6) {
                continue;
            }
            Instant candidate = day.with(LocalTime.of(8, 0)).plusMinutes(random.nextInt(11 * 60)).toInstant();
            if (candidate.isBefore(now)) {
                return candidate;
            }
        }
    }

    private static Priority randomPriority(Random random) {
        int roll = random.nextInt(100);
        if (roll < 8) {
            return Priority.CRITICA;
        }
        if (roll < 30) {
            return Priority.ALTA;
        }
        return roll < 78 ? Priority.MEDIA : Priority.BAJA;
    }

    private static Duration scale(Duration duration, double factor) {
        return Duration.ofSeconds((long) (duration.toSeconds() * factor));
    }
}
