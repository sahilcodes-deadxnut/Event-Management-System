package com.eventhub.servlet;

import com.eventhub.dao.RegistrationJdbcDao;
import com.eventhub.model.Registration;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

@Component
public class TicketBadgeQrServlet extends HttpServlet {

    // JDBC DAO used to retrieve verified ticket registration details
    private final RegistrationJdbcDao registrationJdbcDao;

    @Autowired
    public TicketBadgeQrServlet(RegistrationJdbcDao registrationJdbcDao) {
        // Inject registration DAO dependency
        this.registrationJdbcDao = registrationJdbcDao;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Extract registration ticket code from request parameter
        String code = request.getParameter("code");

        // Validate that code parameter is provided
        if (code == null || code.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required 'code' parameter.");
            return;
        }

        String normalizedCode = code.trim().toUpperCase();
        // Query ticket pass from database
        Optional<Registration> regOpt = registrationJdbcDao.findByCode(normalizedCode);

        // Fallback default info if code is not found in database yet
        String participantName = regOpt.map(Registration::getParticipantName).orElse("Attendee Pass");
        String eventTitle = regOpt.map(r -> r.getEvent() != null ? r.getEvent().getTitle() : "EventHub Pass").orElse("EventHub Event");

        // Configure HTTP response header for Scalable Vector Graphics (SVG)
        response.setContentType("image/svg+xml; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        // Instruct browser to cache badge for 1 hour
        response.setHeader("Cache-Control", "public, max-age=3600");

        // Generate deterministic 21x21 pseudo-QR grid based on SHA-256 hash of the code
        boolean[][] matrix = generateQrMatrix(normalizedCode);

        // Stream generated SVG XML to client
        PrintWriter out = response.getWriter();
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 400 480\" width=\"400\" height=\"480\">");
        out.println("  <defs>");
        out.println("    <linearGradient id=\"headerGrad\" x1=\"0%\" y1=\"0%\" x2=\"100%\" y2=\"100%\">");
        out.println("      <stop offset=\"0%\" stop-color=\"#4f46e5\" />");
        out.println("      <stop offset=\"100%\" stop-color=\"#06b6d4\" />");
        out.println("    </linearGradient>");
        out.println("    <filter id=\"shadow\" x=\"-5%\" y=\"-5%\" width=\"110%\" height=\"110%\">");
        out.println("      <feDropShadow dx=\"0\" dy=\"4\" stdDeviation=\"6\" flood-opacity=\"0.15\" />");
        out.println("    </filter>");
        out.println("  </defs>");

        // Background card
        out.println("  <rect x=\"10\" y=\"10\" width=\"380\" height=\"460\" rx=\"20\" fill=\"#ffffff\" stroke=\"#e2e8f0\" stroke-width=\"2\" filter=\"url(#shadow)\" />");

        // Card header banner
        out.println("  <path d=\"M 10 30 Q 10 10 30 10 L 370 10 Q 390 10 390 30 L 390 85 L 10 85 Z\" fill=\"url(#headerGrad)\" />");
        out.println("  <text x=\"200\" y=\"45\" text-anchor=\"middle\" fill=\"#ffffff\" font-family=\"Arial, sans-serif\" font-size=\"14\" font-weight=\"bold\" letter-spacing=\"2\">EVENT ADMISSION PASS</text>");
        out.println("  <text x=\"200\" y=\"68\" text-anchor=\"middle\" fill=\"#e0e7ff\" font-family=\"Arial, sans-serif\" font-size=\"12\">EventHub Digital Check-in</text>");

        // Event and participant details
        out.println("  <text x=\"200\" y=\"115\" text-anchor=\"middle\" fill=\"#1e293b\" font-family=\"Arial, sans-serif\" font-size=\"16\" font-weight=\"bold\">" + escapeXml(participantName) + "</text>");
        out.println("  <text x=\"200\" y=\"135\" text-anchor=\"middle\" fill=\"#64748b\" font-family=\"Arial, sans-serif\" font-size=\"12\">" + escapeXml(eventTitle) + "</text>");

        // QR Code Container Box
        out.println("  <rect x=\"90\" y=\"155\" width=\"220\" height=\"220\" rx=\"12\" fill=\"#f8fafc\" stroke=\"#cbd5e1\" stroke-width=\"1.5\" />");

        // Render QR modules (matrix 21x21 modules, 8px each with 26px padding inside 220px box)
        int startX = 104;
        int startY = 169;
        int cellSize = 9;

        out.println("  <g fill=\"#0f172a\">");
        for (int r = 0; r < 21; r++) {
            for (int c = 0; c < 21; c++) {
                if (matrix[r][c]) {
                    int x = startX + c * cellSize;
                    int y = startY + r * cellSize;
                    out.printf("    <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"1.5\" />%n", x, y, cellSize, cellSize);
                }
            }
        }
        out.println("  </g>");

        // Ticket Reference Code Box
        out.println("  <rect x=\"50\" y=\"395\" width=\"300\" height=\"44\" rx=\"8\" fill=\"#eff6ff\" stroke=\"#bfdbfe\" stroke-width=\"1\" />");
        out.println("  <text x=\"200\" y=\"422\" text-anchor=\"middle\" fill=\"#1d4ed8\" font-family=\"monospace\" font-size=\"18\" font-weight=\"bold\" letter-spacing=\"3\">" + escapeXml(normalizedCode) + "</text>");
        out.println("  <text x=\"200\" y=\"458\" text-anchor=\"middle\" fill=\"#94a3b8\" font-family=\"Arial, sans-serif\" font-size=\"10\">SCAN AT VENUE GATE TO VERIFY ENTRY</text>");
        out.println("</svg>");
        out.flush();
    }

    private boolean[][] generateQrMatrix(String data) {
        boolean[][] m = new boolean[21][21];

        // Draw standard QR Finder Pattern top-left
        drawFinder(m, 0, 0);
        // Draw standard QR Finder Pattern top-right
        drawFinder(m, 0, 14);
        // Draw standard QR Finder Pattern bottom-left
        drawFinder(m, 14, 0);

        // Draw timing lines
        for (int i = 8; i < 13; i++) {
            m[6][i] = (i % 2 == 0);
            m[i][6] = (i % 2 == 0);
        }

        // Generate deterministic payload pattern from SHA-256 hash of ticket code
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            int bitIndex = 0;

            for (int r = 0; r < 21; r++) {
                for (int c = 0; c < 21; c++) {
                    // Skip finder patterns
                    if (isFinderArea(r, c)) continue;
                    // Skip timing lines
                    if (r == 6 || c == 6) continue;

                    int bytePos = (bitIndex / 8) % hash.length;
                    int bitPos = bitIndex % 8;
                    m[r][c] = ((hash[bytePos] >> bitPos) & 1) == 1;
                    bitIndex++;
                }
            }
        } catch (Exception ex) {
            // Fallback deterministic pattern
            for (int r = 8; r < 21; r++) {
                for (int c = 8; c < 21; c++) {
                    m[r][c] = (r * c + data.hashCode()) % 2 == 0;
                }
            }
        }

        return m;
    }

    private void drawFinder(boolean[][] m, int row, int col) {
        // Outer 7x7 black box
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                if (r == 0 || r == 6 || c == 0 || c == 6 || (r >= 2 && r <= 4 && c >= 2 && c <= 4)) {
                    m[row + r][col + c] = true;
                }
            }
        }
    }

    private boolean isFinderArea(int r, int c) {
        // Top-left finder area
        if (r < 8 && c < 8) return true;
        // Top-right finder area
        if (r < 8 && c > 12) return true;
        // Bottom-left finder area
        if (r > 12 && c < 8) return true;
        return false;
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
