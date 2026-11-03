package com.eventhub.servlet;

import com.eventhub.model.Event;
import com.eventhub.model.Registration;
import com.eventhub.service.EventService;
import com.eventhub.service.RegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@Component
public class EventJsonApiServlet extends HttpServlet {

    // Services providing business logic and database persistence
    private final EventService eventService;
    private final RegistrationService registrationService;
    private final ObjectMapper objectMapper;

    @Autowired
    public EventJsonApiServlet(EventService eventService,
                               RegistrationService registrationService,
                               ObjectMapper objectMapper) {
        // Inject dependencies for API operations
        this.eventService = eventService;
        this.registrationService = registrationService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Configure HTTP response headers for JSON payload
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        // Extract extra path information e.g. /123
        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();

        // Scenario 1: Specific event lookup by ID e.g. GET /api/events/1
        if (pathInfo != null && pathInfo.length() > 1) {
            String idStr = pathInfo.substring(1);
            try {
                Long eventId = Long.parseLong(idStr);
                Optional<Event> eventOpt = eventService.getEventById(eventId);

                if (eventOpt.isPresent()) {
                    Event e = eventOpt.get();
                    Map<String, Object> map = buildEventDto(e);
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.print(objectMapper.writeValueAsString(map));
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(objectMapper.writeValueAsString(Map.of("error", "Event not found with ID: " + eventId)));
                }
            } catch (NumberFormatException nfe) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(objectMapper.writeValueAsString(Map.of("error", "Invalid event ID format.")));
            }
            out.flush();
            return;
        }

        // Scenario 2: List and filter events e.g. GET /api/events?category=Technical&search=AI
        String search = request.getParameter("search");
        String category = request.getParameter("category");

        List<Event> events = eventService.searchAndFilterEvents(search, category);
        List<Map<String, Object>> dtoList = new ArrayList<>();

        for (Event e : events) {
            dtoList.add(buildEventDto(e));
        }

        response.setStatus(HttpServletResponse.SC_OK);
        out.print(objectMapper.writeValueAsString(dtoList));
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Configure response headers
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();

        // Path format must be /{id}/register
        if (pathInfo == null || !pathInfo.matches("/\\d+/register")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(objectMapper.writeValueAsString(Map.of("error", "Expected endpoint URL format: POST /api/events/{id}/register")));
            out.flush();
            return;
        }

        // Extract event ID from path
        String[] parts = pathInfo.split("/");
        Long eventId = Long.parseLong(parts[1]);

        try {
            // Read and parse incoming JSON payload or fallback to request parameters
            Registration reg = new Registration();

            String contentType = request.getContentType();
            if (contentType != null && contentType.toLowerCase().contains("application/json")) {
                StringBuilder sb = new StringBuilder();
                String line;
                try (BufferedReader reader = request.getReader()) {
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                @SuppressWarnings("unchecked")
                Map<String, String> body = objectMapper.readValue(sb.toString(), Map.class);
                reg.setParticipantName(body.get("name"));
                reg.setEmail(body.get("email"));
                reg.setPhone(body.get("phone"));
                reg.setOrganization(body.get("organization"));
                reg.setRoleTitle(body.get("roleTitle"));
                reg.setSpecialNotes(body.get("specialNotes"));
            } else {
                reg.setParticipantName(request.getParameter("name"));
                reg.setEmail(request.getParameter("email"));
                reg.setPhone(request.getParameter("phone"));
                reg.setOrganization(request.getParameter("organization"));
                reg.setRoleTitle(request.getParameter("roleTitle"));
                reg.setSpecialNotes(request.getParameter("specialNotes"));
            }

            // Enforce mandatory fields
            if (reg.getParticipantName() == null || reg.getParticipantName().trim().isEmpty() ||
                reg.getEmail() == null || reg.getEmail().trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(objectMapper.writeValueAsString(Map.of("error", "Fields 'name' and 'email' are required.")));
                out.flush();
                return;
            }

            // Register participant with synchronized concurrency control and duplicate prevention
            Registration saved = registrationService.registerParticipant(eventId, reg);

            // Respond with HTTP 201 Created and ticket details
            response.setStatus(HttpServletResponse.SC_CREATED);
            Map<String, Object> respMap = new LinkedHashMap<>();
            respMap.put("status", "CONFIRMED");
            respMap.put("ticketCode", saved.getRegistrationCode());
            respMap.put("eventId", eventId);
            respMap.put("participantName", saved.getParticipantName());
            respMap.put("email", saved.getEmail());
            respMap.put("registrationDate", saved.getFormattedDate());
            respMap.put("badgeUrl", "/servlets/ticket-badge?code=" + saved.getRegistrationCode());

            out.print(objectMapper.writeValueAsString(respMap));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            out.print(objectMapper.writeValueAsString(Map.of("error", ex.getMessage())));
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(objectMapper.writeValueAsString(Map.of("error", "Unable to complete registration: " + ex.getMessage())));
        }

        out.flush();
    }

    private Map<String, Object> buildEventDto(Event e) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", e.getId());
        map.put("title", e.getTitle());
        map.put("category", e.getCategory());
        map.put("date", e.getFormattedDate());
        map.put("venue", e.getVenue());
        map.put("capacity", e.getCapacity());
        map.put("registeredCount", e.getRegisteredCount());
        map.put("seatsLeft", e.getSeatsLeft());
        map.put("isFull", e.isFull());
        map.put("status", e.getStatus());
        map.put("organizerName", e.getOrganizerName());
        return map;
    }
}
