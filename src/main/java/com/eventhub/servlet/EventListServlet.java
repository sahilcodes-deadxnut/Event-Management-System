package com.eventhub.servlet;

import com.eventhub.dao.EventJdbcDao;
import com.eventhub.model.Event;
import jakarta.servlet.ServletConfig;
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
public class EventListServlet extends HttpServlet {

    // JDBC DAO dependency to retrieve events directly via raw SQL
    private final EventJdbcDao eventJdbcDao;

    @Autowired
    public EventListServlet(EventJdbcDao eventJdbcDao) {
        // Inject EventJdbcDao instance
        this.eventJdbcDao = eventJdbcDao;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        // Invoke superclass init lifecycle method
        super.init(config);
        // Log servlet initialization event
        log("[SERVLET INIT] EventListServlet initialized and ready to handle HTTP requests.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Retrieve existing HTTP session or create a new session if none exists
        HttpSession session = request.getSession(true);

        // Track and increment total page visits count within this client session
        Integer visitCount = (Integer) session.getAttribute("servletVisitCount");
        visitCount = (visitCount == null) ? 1 : visitCount + 1;
        session.setAttribute("servletVisitCount", visitCount);

        // Fetch all events from database using direct JDBC DAO
        List<Event> events = eventJdbcDao.findAll();

        // Configure response content type to HTML with UTF-8 character encoding
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        // Obtain response writer to output HTML document
        PrintWriter out = response.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("  <meta charset='UTF-8'>");
        out.println("  <title>Event Catalog (Servlet &amp; JDBC)</title>");
        out.println("  <link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css' rel='stylesheet'>");
        out.println("</head>");
        out.println("<body class='bg-light'>");
        out.println("  <div class='container py-5'>");

        // Header section displaying session visit tracking
        out.println("    <div class='d-flex justify-content-between align-items-center mb-4'>");
        out.println("      <div>");
        out.println("        <h2 class='fw-bold mb-1'>🎪 Event Catalog (Java Servlet API)</h2>");
        out.println("        <p class='text-muted mb-0'>Rendered directly by <code>HttpServlet</code> + <code>EventJdbcDao</code></p>");
        out.println("      </div>");
        out.println("      <div>");
        out.println("        <span class='badge bg-primary px-3 py-2'>Session Visits: " + visitCount + "</span>");
        out.println("        <a href='/' class='btn btn-outline-secondary btn-sm ms-2'>Home Portal</a>");
        out.println("      </div>");
        out.println("    </div>");

        // Event listings table
        out.println("    <div class='card shadow-sm border-0 rounded-3'>");
        out.println("      <div class='table-responsive'>");
        out.println("        <table class='table table-hover align-middle mb-0'>");
        out.println("          <thead class='table-dark'>");
        out.println("            <tr>");
        out.println("              <th>ID</th>");
        out.println("              <th>Title</th>");
        out.println("              <th>Category</th>");
        out.println("              <th>Venue</th>");
        out.println("              <th>Capacity</th>");
        out.println("              <th>Status</th>");
        out.println("              <th class='text-end'>Action</th>");
        out.println("            </tr>");
        out.println("          </thead>");
        out.println("          <tbody>");

        // Iterate through events and render each row
        for (Event e : events) {
            out.println("            <tr>");
            out.println("              <td>#" + e.getId() + "</td>");
            out.println("              <td class='fw-bold'>" + escapeHtml(e.getTitle()) + "</td>");
            out.println("              <td><span class='badge bg-info text-dark'>" + escapeHtml(e.getCategory()) + "</span></td>");
            out.println("              <td>" + escapeHtml(e.getVenue()) + "</td>");
            out.println("              <td>" + e.getCapacity() + " seats</td>");
            out.println("              <td><span class='badge bg-success'>" + e.getStatus() + "</span></td>");
            out.println("              <td class='text-end'>");
            out.println("                <a href='/servlets/register?eventId=" + e.getId() + "' class='btn btn-primary btn-sm'>Register (Servlet)</a>");
            out.println("              </td>");
            out.println("            </tr>");
        }

        out.println("          </tbody>");
        out.println("        </table>");
        out.println("      </div>");
        out.println("    </div>");

        out.println("  </div>");
        out.println("</body>");
        out.println("</html>");

        // Flush output buffer
        out.flush();
    }

    @Override
    public void destroy() {
        // Log servlet destruction lifecycle event
        log("[SERVLET DESTROY] EventListServlet destroyed.");
        super.destroy();
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
