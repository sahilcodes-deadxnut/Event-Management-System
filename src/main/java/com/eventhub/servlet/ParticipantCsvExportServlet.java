package com.eventhub.servlet;

import com.eventhub.model.Registration;
import com.eventhub.security.AdminAuthService;
import com.eventhub.service.RegistrationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Component
public class ParticipantCsvExportServlet extends HttpServlet {

    private final RegistrationService registrationService;
    private final AdminAuthService adminAuthService;

    @Autowired
    public ParticipantCsvExportServlet(RegistrationService registrationService, AdminAuthService adminAuthService) {
        this.registrationService = registrationService;
        this.adminAuthService = adminAuthService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Enforce administrator authentication before allowing export of sensitive participant data
        HttpSession session = request.getSession(false);
        if (!adminAuthService.isAuthenticated(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied. Administrator privileges required to export participant rosters.");
            return;
        }

        // Extract the eventId query parameter from the HTTP GET request
        String eventIdParam = request.getParameter("eventId");

        if (eventIdParam == null || eventIdParam.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required 'eventId' parameter.");
            return;
        }

        Long eventId;
        try {
            eventId = Long.parseLong(eventIdParam.trim());
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid numeric format for 'eventId'.");
            return;
        }

        // Query database for all participants registered for this event
        List<Registration> participants = registrationService.getParticipantsByEvent(eventId);

        // Configure HTTP response MIME type to text/csv with UTF-8 character encoding
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        String fileName = "participants_event_" + eventId + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        // Prevent browser and proxy caching of dynamically generated export file
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        try (PrintWriter writer = response.getWriter()) {
            writer.println("Ticket Code,Participant Name,Email,Phone,Organization,Role,Registration Date");

            for (Registration reg : participants) {
                String code = escapeCsv(reg.getRegistrationCode());
                String name = escapeCsv(reg.getParticipantName());
                String email = escapeCsv(reg.getEmail());
                String phone = escapeCsv(reg.getPhone());
                String org = escapeCsv(reg.getOrganization());
                String role = escapeCsv(reg.getRoleTitle());
                String date = escapeCsv(reg.getFormattedDate());

                writer.printf("%s,%s,%s,%s,%s,%s,%s%n", code, name, email, phone, org, role, date);
            }

            writer.flush();
        }
    }

    /**
     * Escapes CSV fields compliant with RFC-4180 and immunizes against
     * CSV Formula Injection (DDE) by prefixing dangerous formula symbols (=, +, -, @).
     */
    public static String escapeCsv(String value) {
        if (value == null) {
            return "\"\"";
        }
        String trimmed = value.trim();
        // Protect against spreadsheet formula injection (CWE-1236)
        if (!trimmed.isEmpty()) {
            char firstChar = trimmed.charAt(0);
            if (firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@' || firstChar == '\t' || firstChar == '\r') {
                trimmed = "'" + trimmed;
            }
        }
        // Double up existing quotes
        String escaped = trimmed.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
