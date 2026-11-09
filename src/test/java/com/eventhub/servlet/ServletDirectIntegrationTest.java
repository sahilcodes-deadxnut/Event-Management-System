package com.eventhub.servlet;

import com.eventhub.model.Event;
import com.eventhub.model.Registration;
import com.eventhub.security.AdminAuthService;
import com.eventhub.security.PasswordHasher;
import com.eventhub.service.EventService;
import com.eventhub.service.RegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ServletDirectIntegrationTest {

    private ParticipantCsvExportServlet csvExportServlet;
    private EventJsonApiServlet jsonApiServlet;
    private AdminAuthService adminAuthService;
    private Event mockEvent;

    @BeforeEach
    void setUp() {
        PasswordHasher hasher = new PasswordHasher();
        adminAuthService = new AdminAuthService(hasher);
        adminAuthService.initializeAdminCredentials();

        mockEvent = new Event(
                "Cloud Architecture Summit",
                "Technology",
                "Scalable microservices and cloud infrastructure",
                LocalDateTime.now().plusDays(14),
                "Auditorium 1",
                100,
                "Sarah Connor",
                "sarah@sky.net",
                "+1-555-0987",
                ""
        );
        mockEvent.setId(42L);

        // Lightweight EventService stub
        EventService eventService = new EventService(null) {
            @Override
            public List<Event> getAllEvents() {
                return Collections.singletonList(mockEvent);
            }

            @Override
            public List<Event> searchAndFilterEvents(String keyword, String category) {
                return Collections.singletonList(mockEvent);
            }

            @Override
            public Optional<Event> getEventById(Long id) {
                if (Long.valueOf(42L).equals(id)) {
                    return Optional.of(mockEvent);
                }
                return Optional.empty();
            }
        };

        // Lightweight RegistrationService stub
        RegistrationService registrationService = new RegistrationService(null, null, null, null) {
            @Override
            public List<Registration> getParticipantsByEvent(Long eventId) {
                Registration reg = new Registration();
                reg.setId(1L);
                reg.setParticipantName("John Connor");
                reg.setEmail("john@res.org");
                reg.setPhone("+1-555-4321");
                reg.setOrganization("Resistance");
                reg.setRoleTitle("Lead");
                reg.setRegistrationCode("EVT-2026-JC01");
                reg.setRegistrationDate(LocalDateTime.now());
                return Collections.singletonList(reg);
            }
        };

        ObjectMapper objectMapper = new ObjectMapper();
        csvExportServlet = new ParticipantCsvExportServlet(registrationService, adminAuthService);
        jsonApiServlet = new EventJsonApiServlet(eventService, registrationService, objectMapper);
    }

    @Test
    @DisplayName("ParticipantCsvExportServlet: Unauthenticated request is rejected with 403 Forbidden")
    void testCsvExportUnauthenticatedRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/servlets/export-csv");
        request.setParameter("eventId", "42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        csvExportServlet.doGet(request, response);

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("ParticipantCsvExportServlet: Authenticated admin request returns CSV with security headers")
    void testCsvExportAuthenticatedSuccess() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/servlets/export-csv");
        request.setParameter("eventId", "42");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AdminAuthService.SESSION_ADMIN_KEY, "admin");
        request.setSession(session);

        MockHttpServletResponse response = new MockHttpServletResponse();

        csvExportServlet.doGet(request, response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).contains("text/csv");
        assertThat(response.getHeader("Content-Disposition")).contains("attachment; filename=\"participants_event_42.csv\"");
        String csvBody = response.getContentAsString();
        assertThat(csvBody).contains("Ticket Code,Participant Name,Email,Phone,Organization,Role,Registration Date");
        assertThat(csvBody).contains("EVT-2026-JC01");
        assertThat(csvBody).contains("John Connor");
        assertThat(csvBody).contains("'+1-555-4321");
    }

    @Test
    @DisplayName("EventJsonApiServlet: GET /api/events returns JSON array of events")
    void testEventJsonApiGetAll() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jsonApiServlet.doGet(request, response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).contains("application/json");
        String json = response.getContentAsString();
        assertThat(json).contains("Cloud Architecture Summit");
        assertThat(json).contains("Auditorium 1");
    }

    @Test
    @DisplayName("EventJsonApiServlet: GET /api/events/42 returns JSON details for specific event")
    void testEventJsonApiGetById() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events/42");
        request.setPathInfo("/42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jsonApiServlet.doGet(request, response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).contains("application/json");
        String json = response.getContentAsString();
        assertThat(json).contains("Cloud Architecture Summit");
    }
}
