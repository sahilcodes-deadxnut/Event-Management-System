package com.eventhub.servlet;

import com.eventhub.dao.EventJdbcDao;
import com.eventhub.dao.RegistrationJdbcDao;
import com.eventhub.model.Event;
import com.eventhub.model.Registration;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

@Component
public class EventRegistrationServlet extends HttpServlet {

    // JDBC DAOs for database operations
    private final EventJdbcDao eventJdbcDao;
    private final RegistrationJdbcDao registrationJdbcDao;

    @Autowired
    public EventRegistrationServlet(EventJdbcDao eventJdbcDao, RegistrationJdbcDao registrationJdbcDao) {
        // Inject JDBC DAO dependencies
        this.eventJdbcDao = eventJdbcDao;
        this.registrationJdbcDao = registrationJdbcDao;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Read the target eventId parameter from HTTP request
        String eventIdParam = request.getParameter("eventId");
        if (eventIdParam == null || eventIdParam.trim().isEmpty()) {
            // Redirect to servlet event catalog if eventId is missing
            response.sendRedirect("/servlets/events");
            return;
        }

        Long eventId;
        try {
            // Parse eventId string into Long
            eventId = Long.parseLong(eventIdParam.trim());
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid numeric eventId.");
            return;
        }

        // Query event details via direct JDBC DAO
        Optional<Event> eventOpt = eventJdbcDao.findById(eventId);
        if (eventOpt.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Event not found with ID: " + eventId);
            return;
        }

        Event event = eventOpt.get();

        // Check if there is an error message stored in session from a previous POST
        HttpSession session = request.getSession(false);
        String flashError = (session != null) ? (String) session.getAttribute("servletError") : null;
        if (session != null) {
            session.removeAttribute("servletError");
        }

        // Configure response headers
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("  <meta charset='UTF-8'>");
        out.println("  <title>Register for " + escapeHtml(event.getTitle()) + " (Servlet)</title>");
        out.println("  <link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css' rel='stylesheet'>");
        out.println("</head>");
        out.println("<body class='bg-light'>");
        out.println("  <div class='container py-5' style='max-width: 650px;'>");
        out.println("    <a href='/servlets/events' class='btn btn-outline-secondary btn-sm mb-3'>&larr; Back to Events</a>");

        // Display session flash error message if present
        if (flashError != null) {
            out.println("    <div class='alert alert-danger'>" + escapeHtml(flashError) + "</div>");
        }

        // Event information banner
        out.println("    <div class='card border-0 shadow-sm rounded-4 p-4 mb-4 bg-white'>");
        out.println("      <h3 class='fw-bold mb-1'>" + escapeHtml(event.getTitle()) + "</h3>");
        out.println("      <p class='text-muted mb-2'>Venue: " + escapeHtml(event.getVenue()) + " | Capacity: " + event.getCapacity() + " seats</p>");
        out.println("      <hr>");

        // Registration form submitting via HTTP POST to this servlet
        out.println("      <form action='/servlets/register' method='POST'>");
        out.println("        <input type='hidden' name='eventId' value='" + event.getId() + "'>");

        out.println("        <div class='mb-3'>");
        out.println("          <label class='form-label fw-semibold'>Full Name *</label>");
        out.println("          <input type='text' name='name' class='form-control' required placeholder='Enter your name'>");
        out.println("        </div>");

        out.println("        <div class='mb-3'>");
        out.println("          <label class='form-label fw-semibold'>Email Address *</label>");
        out.println("          <input type='email' name='email' class='form-control' required placeholder='name@example.com'>");
        out.println("        </div>");

        out.println("        <div class='mb-3'>");
        out.println("          <label class='form-label fw-semibold'>Phone Number *</label>");
        out.println("          <input type='text' name='phone' class='form-control' required placeholder='+1 555-0199'>");
        out.println("        </div>");

        out.println("        <div class='row g-2 mb-3'>");
        out.println("          <div class='col-sm-6'>");
        out.println("            <label class='form-label small text-muted'>Organization</label>");
        out.println("            <input type='text' name='organization' class='form-control' placeholder='Company / College'>");
        out.println("          </div>");
        out.println("          <div class='col-sm-6'>");
        out.println("            <label class='form-label small text-muted'>Role Title</label>");
        out.println("            <input type='text' name='roleTitle' class='form-control' placeholder='Developer / Student'>");
        out.println("          </div>");
        out.println("        </div>");

        out.println("        <div class='mb-3'>");
        out.println("          <label class='form-label small text-muted'>Special Notes</label>");
        out.println("          <textarea name='specialNotes' class='form-control' rows='2' placeholder='Dietary or accessibility notes'></textarea>");
        out.println("        </div>");

        out.println("        <button type='submit' class='btn btn-primary w-100 py-2 fw-bold'>Submit Registration (JDBC Transaction)</button>");
        out.println("      </form>");
        out.println("    </div>");
        out.println("  </div>");
        out.println("</body>");
        out.println("</html>");

        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Read form parameters submitted through POST
        String eventIdParam = request.getParameter("eventId");
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String organization = request.getParameter("organization");
        String roleTitle = request.getParameter("roleTitle");
        String specialNotes = request.getParameter("specialNotes");

        // Parse event ID
        Long eventId;
        try {
            eventId = Long.parseLong(eventIdParam);
        } catch (Exception ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid event ID.");
            return;
        }

        // Basic input validation
        if (name == null || name.trim().isEmpty() || email == null || email.trim().isEmpty()) {
            HttpSession session = request.getSession(true);
            session.setAttribute("servletError", "Name and email are mandatory fields.");
            response.sendRedirect("/servlets/register?eventId=" + eventId);
            return;
        }

        // Construct participant domain model
        Registration reg = new Registration();
        reg.setParticipantName(name.trim());
        reg.setEmail(email.trim());
        reg.setPhone(phone != null ? phone.trim() : "");
        reg.setOrganization(organization);
        reg.setRoleTitle(roleTitle);
        reg.setSpecialNotes(specialNotes);

        try {
            // Process registration using explicit JDBC transaction management (commit & rollback)
            Registration saved = registrationJdbcDao.registerParticipantTransactional(eventId, reg);

            // Render confirmation pass
            response.setContentType("text/html; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();

            out.println("<!DOCTYPE html>");
            out.println("<html><head><meta charset='UTF-8'><title>Pass Confirmed</title>");
            out.println("<link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css' rel='stylesheet'></head>");
            out.println("<body class='bg-light py-5'>");
            out.println("<div class='container' style='max-width: 550px;'>");
            out.println("  <div class='card border-0 shadow-lg rounded-4 p-4 text-center bg-white'>");
            out.println("    <div class='text-success fs-1 mb-2'>&#10004;</div>");
            out.println("    <h3 class='fw-bold mb-1'>Registration Confirmed!</h3>");
            out.println("    <p class='text-muted small'>Processed via raw JDBC Transaction</p>");
            out.println("    <div class='bg-light p-3 rounded-3 my-3'>");
            out.println("      <small class='text-uppercase text-muted fw-bold'>Ticket Reference Code</small>");
            out.println("      <h2 class='fw-bold text-primary mb-0'>" + saved.getRegistrationCode() + "</h2>");
            out.println("    </div>");
            out.println("    <p><strong>Name:</strong> " + escapeHtml(saved.getParticipantName()) + "<br><strong>Email:</strong> " + escapeHtml(saved.getEmail()) + "</p>");
            out.println("    <div class='d-flex gap-2 justify-content-center mt-3'>");
            out.println("      <a href='/servlets/events' class='btn btn-outline-primary'>All Events</a>");
            out.println("      <a href='/' class='btn btn-primary'>Home Portal</a>");
            out.println("    </div>");
            out.println("  </div>");
            out.println("</div>");
            out.println("</body></html>");
            out.flush();

        } catch (Exception ex) {
            // Store error in session and redirect back to form
            HttpSession session = request.getSession(true);
            session.setAttribute("servletError", ex.getMessage());
            response.sendRedirect("/servlets/register?eventId=" + eventId);
        }
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
