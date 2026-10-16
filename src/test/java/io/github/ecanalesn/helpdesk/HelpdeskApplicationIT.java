package io.github.ecanalesn.helpdesk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Aplicación completa (Spring + JPA + H2 + Thymeleaf + datos de demostración) con un reloj fijo.
 * Cada test hace rollback, así que los datos de demostración no cambian entre tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
@Transactional
class HelpdeskApplicationIT {

    @Autowired
    private MockMvc mvc;

    @Test
    void dashboardRendersWithDemoData() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Panel de soporte")))
                .andExpect(content().string(containsString("Octubre 2026")));
    }

    @Test
    void metricsApiReturnsTheMonth() throws Exception {
        mvc.perform(get("/api/metrics").param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-10"))
                .andExpect(jsonPath("$.summary.created").value(greaterThan(0)))
                .andExpect(jsonPath("$.daily", hasSize(16)))
                .andExpect(jsonPath("$.byPriority", hasSize(4)))
                .andExpect(jsonPath("$.byCategory", hasSize(6)));
    }

    @Test
    void servesFrontendDependenciesFromWebJars() throws Exception {
        mvc.perform(get("/webjars/bootstrap/dist/css/bootstrap.min.css")).andExpect(status().isOk());
        mvc.perform(get("/webjars/chart.js/dist/chart.umd.min.js")).andExpect(status().isOk());
        mvc.perform(get("/js/dashboard.js")).andExpect(status().isOk());
    }

    @Test
    void ticketLifecycleThroughTheApi() throws Exception {
        MvcResult created = mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content("""
                        {"title": "La pipeline falla en el paso de tests", "requester": "Hugo",
                         "category": "INTEGRACION_CONTINUA", "priority": "CRITICA"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABIERTO"))
                .andReturn();
        String location = created.getResponse().getHeader("Location");
        assertNotNull(location);

        mvc.perform(post(location + "/start").contentType(MediaType.APPLICATION_JSON).content("{\"assignee\": \"Estefanía\"}"))
                .andExpect(jsonPath("$.status").value("EN_CURSO"));
        mvc.perform(post(location + "/resolve"))
                .andExpect(jsonPath("$.status").value("RESUELTO"))
                .andExpect(jsonPath("$.resolutionMinutes").value(0));
        mvc.perform(post(location + "/resolve"))
                .andExpect(status().isConflict());
        mvc.perform(post(location + "/reopen"))
                .andExpect(jsonPath("$.status").value("EN_CURSO"));
    }

    @Test
    void apiErrorsUseProblemDetails() throws Exception {
        mvc.perform(get("/api/tickets/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void webShowsHtml404ForUnknownTicket() throws Exception {
        mvc.perform(get("/tickets/999999").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound());
    }

    @Test
    void createsTicketFromTheWebForm() throws Exception {
        MvcResult result = mvc.perform(post("/tickets")
                        .param("title", "Configurar el mirror corporativo en settings.xml")
                        .param("requester", "Clara")
                        .param("category", "BUILD")
                        .param("priority", "MEDIA"))
                .andExpect(redirectedUrlPattern("/tickets/*"))
                .andReturn();

        mvc.perform(get(result.getResponse().getRedirectedUrl()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Configurar el mirror corporativo en settings.xml")));
    }

    @Test
    void ticketListFiltersByStatus() throws Exception {
        mvc.perform(get("/tickets").param("status", "ABIERTO").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("tickets")));
    }

    @Test
    void downloadsTheMonthlyReportAsCsv() throws Exception {
        MvcResult result = mvc.perform(get("/reports/monthly.csv").param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", containsString("informe-helpdesk-2026-10.csv")))
                .andReturn();

        byte[] body = result.getResponse().getContentAsByteArray();
        assertArrayEquals(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, Arrays.copyOf(body, 3));
        assertThat(new String(body, StandardCharsets.UTF_8),
                containsString("Informe mensual de soporte;2026-10"));
    }

    @Test
    void actuatorHealthIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
