package com.eventhub.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class PptxBuilder {

    private static final String TEMPLATE_PATH = "C:/Users/Sahil/.gemini/antigravity/scratch/event-management-system/Event_Management_System_Presentation.pptx";
    private static final String OUTPUT_PATH = "C:/Users/Sahil/.gemini/antigravity/scratch/event-management-system/Event_Management_System_Presentation.pptx";

    public static void main(String[] args) throws Exception {
        System.out.println("Building 13-Slide Comprehensive Presentation with Code Showcases...");

        File templateFile = new File(TEMPLATE_PATH);
        if (!templateFile.exists()) {
            System.err.println("Template not found: " + TEMPLATE_PATH);
            return;
        }

        // Read all existing entries from template PPTX into memory
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipFile zip = new ZipFile(templateFile)) {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry ze = en.nextElement();
                if (!ze.isDirectory()) {
                    try (InputStream is = zip.getInputStream(ze)) {
                        entries.put(ze.getName(), is.readAllBytes());
                    }
                }
            }
        }

        // Generate slides 1 to 13
        int totalSlides = 13;
        List<String> slideXmls = new ArrayList<>();

        slideXmls.add(buildSlide1Title());
        slideXmls.add(buildSlide2ProblemSolution());
        slideXmls.add(buildSlide3Architecture());
        slideXmls.add(buildSlide4MultithreadingConcept());
        slideXmls.add(buildSlide5MultithreadingCode());
        slideXmls.add(buildSlide6JdbcConcept());
        slideXmls.add(buildSlide7JdbcCode());
        slideXmls.add(buildSlide8ServletConcept());
        slideXmls.add(buildSlide9ServletApiCode());
        slideXmls.add(buildSlide10SseAndQrCode());
        slideXmls.add(buildSlide11CodeQuality());
        slideXmls.add(buildSlide12Innovation());
        slideXmls.add(buildSlide13Summary());

        // Update entries for all slides
        for (int i = 1; i <= totalSlides; i++) {
            entries.put("ppt/slides/slide" + i + ".xml", slideXmls.get(i - 1).getBytes(StandardCharsets.UTF_8));
            // Ensure slide relationship exists
            String rel = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>\n" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout\" Target=\"../slideLayouts/slideLayout7.xml\"/>" +
                    "</Relationships>";
            entries.put("ppt/slides/_rels/slide" + i + ".xml.rels", rel.getBytes(StandardCharsets.UTF_8));
        }

        // Update [Content_Types].xml
        String contentTypes = buildContentTypes(totalSlides);
        entries.put("[Content_Types].xml", contentTypes.getBytes(StandardCharsets.UTF_8));

        // Update ppt/presentation.xml
        String presentationXml = buildPresentationXml(totalSlides);
        entries.put("ppt/presentation.xml", presentationXml.getBytes(StandardCharsets.UTF_8));

        // Update ppt/_rels/presentation.xml.rels
        String presRels = buildPresentationRels(totalSlides);
        entries.put("ppt/_rels/presentation.xml.rels", presRels.getBytes(StandardCharsets.UTF_8));

        // Write out back to PPTX file
        File tempOutput = new File(OUTPUT_PATH + ".tmp");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempOutput))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                ZipEntry ze = new ZipEntry(entry.getKey());
                zos.putNextEntry(ze);
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }

        // Atomic replace
        Files.move(tempOutput.toPath(), Paths.get(OUTPUT_PATH), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Successfully generated 13-slide PowerPoint at: " + OUTPUT_PATH);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String buildHeader(String title, String subtitle) {
        return "<p:sp><p:nvSpPr><p:cNvPr id=\"2\" name=\"Header\"/><p:cNvSpPr txBox=\"1\"/><p:nvPr/></p:nvSpPr>" +
                "<p:spPr><a:xfrm><a:off x=\"731520\" y=\"457200\"/><a:ext cx=\"10698480\" cy=\"1097280\"/></a:xfrm>" +
                "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom><a:noFill/></p:spPr>" +
                "<p:txBody><a:bodyPr/><a:lstStyle/>" +
                "<a:p><a:r><a:rPr lang=\"en-US\" sz=\"2800\" b=\"1\"><a:solidFill><a:srgbClr val=\"1A2B4C\"/></a:solidFill><a:latin typeface=\"Calibri\"/></a:rPr><a:t>" + escape(title) + "</a:t></a:r></a:p>" +
                "<a:p><a:pPr><a:spcBfr><a:spcPts val=\"400\"/></a:spcBfr></a:pPr><a:r><a:rPr lang=\"en-US\" sz=\"1400\"><a:solidFill><a:srgbClr val=\"64748B\"/></a:solidFill><a:latin typeface=\"Calibri\"/></a:rPr><a:t>" + escape(subtitle) + "</a:t></a:r></a:p>" +
                "</p:txBody></p:sp>";
    }

    private static String buildSlide1Title() {
        return "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>\n" +
                "<p:sld xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/><a:chOff x=\"0\" y=\"0\"/><a:chExt cx=\"0\" cy=\"0\"/></a:xfrm></p:grpSpPr>" +
                "<p:sp><p:nvSpPr><p:cNvPr id=\"2\" name=\"Bg\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr><p:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"12192000\" cy=\"6858000\"/></a:xfrm><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom><a:solidFill><a:srgbClr val=\"1A2B4C\"/></a:solidFill><a:ln><a:noFill/></a:ln></p:spPr></p:sp>" +
                "<p:sp><p:nvSpPr><p:cNvPr id=\"3\" name=\"TitleBox\"/><p:cNvSpPr txBox=\"1\"/><p:nvPr/></p:nvSpPr><p:spPr><a:xfrm><a:off x=\"1097280\" y=\"1645920\"/><a:ext cx=\"10058400\" cy=\"3657600\"/></a:xfrm><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom><a:noFill/></p:spPr><p:txBody><a:bodyPr/><a:lstStyle/>" +
                "<a:p><a:r><a:rPr lang=\"en-US\" sz=\"1400\" b=\"1\"><a:solidFill><a:srgbClr val=\"93C5FD\"/></a:solidFill><a:latin typeface=\"Calibri\"/></a:rPr><a:t>FULL-STACK ENTERPRISE PLATFORM</a:t></a:r></a:p>" +
                "<a:p><a:pPr><a:spcBfr><a:spcPts val=\"1200\"/></a:spcBfr></a:pPr><a:r><a:rPr lang=\"en-US\" sz=\"4400\" b=\"1\"><a:solidFill><a:srgbClr val=\"FFFFFF\"/></a:solidFill><a:latin typeface=\"Calibri\"/></a:rPr><a:t>Event Management System</a:t></a:r></a:p>" +
                "<a:p><a:pPr><a:spcBfr><a:spcPts val=\"1000\"/></a:spcBfr></a:pPr><a:r><a:rPr lang=\"en-US\" sz=\"2000\"><a:solidFill><a:srgbClr val=\"E2E8F0\"/></a:solidFill><a:latin typeface=\"Calibri\"/></a:rPr><a:t>Centralized Planning, Registration, Live Capacity &amp; Participant Management</a:t></a:r></a:p>" +
                "<a:p><a:pPr><a:spcBfr><a:spcPts val=\"3600\"/></a:spcBfr></a:pPr><a:r><a:rPr lang=\"en-US\" sz=\"1400\"><a:solidFill><a:srgbClr val=\"94A3B8\"/></a:solidFill><a:latin typeface=\"Calibri\"/></a:rPr><a:t>Java 21  •  Spring Boot 3  •  Jakarta Servlet API 6.0  •  Direct JDBC DAO  •  Concurrency Locks</a:t></a:r></a:p>" +
                "</p:txBody></p:sp>" +
                "</p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sld>";
    }

    private static String buildSlide2ProblemSolution() {
        return buildTwoCardSlide("Problem Statement & Centralized Solution",
                "Addressing real-world event coordination challenges with an integrated system",
                "The Operational Challenge",
                new String[]{
                        "Scattered Forms & Chats: Uncoordinated signups cause confusion and data loss.",
                        "Duplicate Registrations: Repeat submissions inflate headcount and waste seats.",
                        "Accidental Overbooking: Ad-hoc forms cannot enforce live seat capacity.",
                        "Inefficient Participant Tracking: Manual door check-in is slow and error-prone."
                }, "EF4444",
                "The Centralized Solution (EventHub)",
                new String[]{
                        "All-in-One Platform: Single hub for discovery, registration, and management.",
                        "Duplicate Prevention Engine: Case-insensitive email checks at service & DB layers.",
                        "Live Capacity Control: Synchronized seat locks prevent overbooking under high traffic.",
                        "Digital Passes & Lookup: Instant verifiable ticket passes with dynamic SVG QR codes."
                }, "10B981");
    }

    private static String buildSlide3Architecture() {
        return buildTwoCardSlide("System Architecture & Processing Flow",
                "Layered architecture connecting web tier, business logic, and transactional database",
                "Attendee Workflow",
                new String[]{
                        "Browse & Filter: Attendees query catalog by keyword and category.",
                        "Lock Acquisition: BookingLockManager acquires per-event monitor lock.",
                        "Atomic Verification: Checks seat capacity & duplicate email in real-time.",
                        "Pass Issuance: Generates unique ticket code with printable vector SVG QR badge."
                }, "2563EB",
                "Organizer & Administrative Workflow",
                new String[]{
                        "Real-time Dashboard: View live event metrics and aggregate participants.",
                        "Full Event Lifecycle: Create, update schedule, and archive listings.",
                        "Live SSE Stream: Real-time capacity updates pushed via Async Servlet.",
                        "Direct JDBC Analytics: Raw PreparedStatement queries for occupancy metrics."
                }, "1A2B4C");
    }

    private static String buildSlide4MultithreadingConcept() {
        return buildTwoCardSlide("Multithreading & Concurrency Protection",
                "Preventing race conditions and overbooking when multiple users book simultaneously",
                "Per-Event Synchronized Booking Lock",
                new String[]{
                        "Dedicated Lock Monitors: Maintained in a thread-safe ConcurrentHashMap per event ID.",
                        "High Concurrency Throughput: Registrations for DIFFERENT events run fully in parallel.",
                        "Race Condition Immunity: Registrations for the SAME event serialize cleanly.",
                        "Zero Overbooking: Eliminates the last-seat race condition under high load."
                }, "2563EB",
                "Background Daemon Worker Thread",
                new String[]{
                        "Low-Priority Daemon: Runs at Thread.MIN_PRIORITY without blocking JVM shutdown.",
                        "Periodic Audits: Wakes every 60 seconds to inspect active event seat occupancy.",
                        "High-Demand Alerts: Logs warnings when events reach 90% capacity or sell out.",
                        "Clean Lifecycle: Starts via @PostConstruct and terminates gracefully via @PreDestroy."
                }, "10B981");
    }

    private static String buildSlide5MultithreadingCode() {
        String code = "// BookingLockManager.java - Fine-grained per-event lock monitors\n" +
                "public Object getLock(Long eventId) {\n" +
                "    return eventLocks.computeIfAbsent(eventId, id -> new Object());\n" +
                "}\n\n" +
                "// RegistrationService.java - Synchronized seat allocation\n" +
                "Object eventLock = bookingLockManager.getLock(eventId);\n" +
                "synchronized (eventLock) {\n" +
                "    long count = registrationRepository.countByEventId(eventId);\n" +
                "    if (count >= event.getCapacity()) {\n" +
                "        throw new IllegalStateException(\"Event reached max capacity!\");\n" +
                "    }\n" +
                "    if (registrationRepository.existsByEventIdAndEmailIgnoreCase(eventId, email)) {\n" +
                "        throw new IllegalArgumentException(\"Email already registered!\");\n" +
                "    }\n" +
                "    return registrationRepository.save(registration);\n" +
                "}";

        String notes = "Code Highlights:\n\n" +
                "• computeIfAbsent: Atomically creates dedicated lock object per event ID.\n\n" +
                "• synchronized (eventLock): Guarantees mutual exclusion for the specific event without choking other events.\n\n" +
                "• Atomic Check-Then-Act: Both capacity and duplicate email checks occur inside the synchronized block.";

        return buildCodeSlide("Code Showcase: Multithreading & Synchronization",
                "Fine-grained booking lock monitor & synchronized seat validation", code, notes);
    }

    private static String buildSlide6JdbcConcept() {
        return buildTwoCardSlide("Database Integration & Direct JDBC DAO",
                "Raw java.sql queries and explicit ACID transactions alongside Spring Data JPA",
                "Direct JDBC CRUD Operations",
                new String[]{
                        "PreparedStatement Execution: Parameterized queries protect against SQL injection.",
                        "Generated Keys Capture: Retrieves auto-increment primary keys via RETURN_GENERATED_KEYS.",
                        "Zero ORM Overhead: Ideal for high-throughput batch operations and reporting.",
                        "Robust Resource Cleanup: Standard try-with-resources closes Connection, Statement, and ResultSet."
                }, "2563EB",
                "ACID Transaction Management",
                new String[]{
                        "Explicit setAutoCommit(false): Initiates manual transaction boundary.",
                        "Atomic Verification: Checks event status, seat capacity, and email uniqueness.",
                        "Guaranteed Rollback: conn.rollback() reverses state if any step fails.",
                        "Connection Pool Safety: Finally block guarantees connection return to pool."
                }, "10B981");
    }

    private static String buildSlide7JdbcCode() {
        String code = "// RegistrationJdbcDao.java - Transactional registration\n" +
                "Connection conn = null;\n" +
                "try {\n" +
                "    conn = dataSource.getConnection();\n" +
                "    conn.setAutoCommit(false); // Begin ACID Transaction\n\n" +
                "    // 1. Verify capacity & 2. Check duplicate email with PreparedStatements\n" +
                "    PreparedStatement ps = conn.prepareStatement(checkSql);\n" +
                "    ps.setLong(1, eventId);\n" +
                "    // ... validations ...\n\n" +
                "    // 3. Insert registration record\n" +
                "    PreparedStatement psInsert = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);\n" +
                "    psInsert.executeUpdate();\n\n" +
                "    conn.commit(); // Atomically commit all operations\n" +
                "} catch (Exception ex) {\n" +
                "    if (conn != null) conn.rollback(); // Rollback on any failure\n" +
                "    throw new RuntimeException(ex);\n" +
                "} finally {\n" +
                "    if (conn != null) conn.close();\n" +
                "}";

        String notes = "Code Highlights:\n\n" +
                "• setAutoCommit(false): Disables autocommit to enforce strict ACID guarantees.\n\n" +
                "• RETURN_GENERATED_KEYS: Extracts database auto-increment ID immediately after insert.\n\n" +
                "• conn.rollback(): Reverts partial writes if capacity is exceeded or email is duplicate.";

        return buildCodeSlide("Code Showcase: Direct JDBC & Transactions",
                "ACID transaction management with manual commit, rollback, and PreparedStatement", code, notes);
    }

    private static String buildSlide8ServletConcept() {
        return buildTwoCardSlide("Servlet Implementation & Web Integration",
                "Full Jakarta Servlet API 6.0 suite: REST API, Async SSE, CSV streaming, and Filters",
                "Custom HttpServlets",
                new String[]{
                        "EventJsonApiServlet: Pure RESTful JSON API (/api/events/*) handling GET & POST.",
                        "LiveCapacitySseServlet: Async Servlet streaming real-time Server-Sent Events (SSE).",
                        "TicketBadgeQrServlet: Streams dynamic vector SVG QR passes (image/svg+xml).",
                        "ParticipantCsvExportServlet: Streams attendee rosters as downloadable CSV files."
                }, "2563EB",
                "Filters & Lifecycle Listeners",
                new String[]{
                        "RateLimitingSecurityFilter: Enforces 100 req/min sliding window per IP (HTTP 429).",
                        "RequestAuditAndEncodingFilter: Intercepts all requests, enforcing UTF-8 and measuring latency.",
                        "AppSessionListener: HttpSessionListener atomically tracking active web user sessions.",
                        "ServletConfig: Container registration bean defining load-on-startup order."
                }, "1A2B4C");
    }

    private static String buildSlide9ServletApiCode() {
        String code = "// EventJsonApiServlet.java - RESTful JSON API Servlet\n" +
                "@Override\n" +
                "protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {\n" +
                "    resp.setContentType(\"application/json; charset=UTF-8\");\n" +
                "    String pathInfo = req.getPathInfo();\n\n" +
                "    if (pathInfo != null && pathInfo.length() > 1) {\n" +
                "        Long id = Long.parseLong(pathInfo.substring(1));\n" +
                "        Optional<Event> e = eventService.getEventById(id);\n" +
                "        if (e.isPresent()) {\n" +
                "            resp.setStatus(HttpServletResponse.SC_OK); // 200 OK\n" +
                "            resp.getWriter().print(objectMapper.writeValueAsString(buildDto(e.get())));\n" +
                "        } else {\n" +
                "            resp.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404\n" +
                "        }\n" +
                "    } else {\n" +
                "        List<Event> list = eventService.searchAndFilterEvents(req.getParameter(\"search\"), null);\n" +
                "        resp.getWriter().print(objectMapper.writeValueAsString(list));\n" +
                "    }\n" +
                "}";

        String notes = "Code Highlights:\n\n" +
                "• Native HttpServlet: Implements RESTful JSON endpoint without Spring MVC dispatchers.\n\n" +
                "• HTTP Status Codes: Accurately emits 200 OK, 201 Created, 400 Bad Request, and 404 Not Found.\n\n" +
                "• Jackson Integration: Serializes domain models directly to UTF-8 JSON stream.";

        return buildCodeSlide("Code Showcase: RESTful JSON API Servlet",
                "Native HttpServlet handling JSON serialization, path extraction, and status codes", code, notes);
    }

    private static String buildSlide10SseAndQrCode() {
        String code = "// LiveCapacitySseServlet.java - Asynchronous SSE Capacity Stream\n" +
                "resp.setContentType(\"text/event-stream\");\n" +
                "resp.setHeader(\"Cache-Control\", \"no-cache\");\n" +
                "AsyncContext asyncCtx = req.startAsync(req, resp); // Servlet 3.0+ Async\n" +
                "asyncCtx.setTimeout(300_000);\n" +
                "activeContexts.add(asyncCtx);\n\n" +
                "// TicketBadgeQrServlet.java - Dynamic SVG QR Badge Generator\n" +
                "resp.setContentType(\"image/svg+xml; charset=UTF-8\");\n" +
                "boolean[][] matrix = generateQrMatrix(ticketCode); // Deterministic SHA-256 pattern\n" +
                "PrintWriter out = resp.getWriter();\n" +
                "out.println(\"<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 400 480'>\");\n" +
                "out.println(\"  <rect width='380' height='460' rx='20' fill='#ffffff' />\");\n" +
                "// ... render QR cells as SVG <rect> tags ...\n" +
                "out.println(\"</svg>\");";

        String notes = "Code Highlights:\n\n" +
                "• startAsync(): Non-blocking asynchronous processing holding open client streams.\n\n" +
                "• text/event-stream: Pushes capacity updates to browser EventSource in real time.\n\n" +
                "• image/svg+xml: Generates crisp, printable vector QR codes with zero external dependencies.";

        return buildCodeSlide("Code Showcase: Async SSE & Vector SVG QR Badge",
                "Asynchronous Server-Sent Events stream and native SVG vector badge rendering", code, notes);
    }

    private static String buildSlide11CodeQuality() {
        return buildTwoCardSlide("Code Quality, Security & Automated Verification",
                "Defensive engineering, XSS protection, centralized error handling, and self-testing",
                "Security & Defensive Coding",
                new String[]{
                        "XSS Sanitization: SecuritySanitizer strips script tags and normalizes input text.",
                        "Rate Limiting: RateLimitingSecurityFilter answers abusive clients with HTTP 429.",
                        "Global Exception Handler: @ControllerAdvice maps domain errors to friendly alerts.",
                        "Information Leak Defense: Sanitizes error views to prevent stack trace leaks."
                }, "2563EB",
                "Automated Self-Verification Suite",
                new String[]{
                        "SystemVerificationRunner: Executes automated diagnostic checks on startup.",
                        "JDBC CRUD Check: Verifies insert, query by ID, and raw SQL analytics.",
                        "10-Thread Stress Test: Spawns 10 parallel threads contending for 3 seats.",
                        "Verified Safety: Confirms exactly 3 seats booked, 7 rejected, and 0 overbookings."
                }, "10B981");
    }

    private static String buildSlide12Innovation() {
        return buildTwoCardSlide("Innovation / Standout Features",
                "Advanced capabilities differentiating EventHub from traditional web applications",
                "Real-Time Live Capacities & QR Passes",
                new String[]{
                        "Dynamic Vector SVG QR Badge: Scalable, high-DPI gate pass rendered by dedicated servlet.",
                        "Live SSE Capacity Streaming: Real-time progress bars update without page refreshes.",
                        "One-Click CSV Export: Organizers download formatted attendee spreadsheets instantly.",
                        "Dual-Mode Database: Seamlessly runs on MySQL 8 or zero-setup in-memory H2."
                }, "10B981",
                "Developer Experience & Architecture",
                new String[]{
                        "Interactive Dev Menu: Direct navbar access to JSON API, SSE stream, and JDBC analytics.",
                        "Strict ACID Guarantees: Manual transaction control eliminates partial booking writes.",
                        "Zero Race Conditions: Fine-grained per-event locking prevents overbooking.",
                        "Production-Grade Logging: Slf4j audit trail tracking client IPs, verbs, and execution latency."
                }, "2563EB");
    }

    private static String buildSlide13Summary() {
        return "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>\n" +
                "<p:sld xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/><a:chOff x=\"0\" y=\"0\"/><a:chExt cx=\"0\" cy=\"0\"/></a:xfrm></p:grpSpPr>" +
                "<p:sp><p:nvSpPr><p:cNvPr id=\"2\" name=\"Bg\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr><p:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"12192000\" cy=\"6858000\"/></a:xfrm><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom><a:solidFill><a:srgbClr val=\"1A2B4C\"/></a:solidFill><a:ln><a:noFill/></a:ln></p:spPr></p:sp>" +
                "<p:sp><p:nvSpPr><p:cNvPr id=\"3\" name=\"SummaryBox\"/><p:cNvSpPr txBox=\"1\"/><p:nvPr/></p:nvSpPr><p:spPr><a:xfrm><a:off x=\"1097280\" y=\"1097280\"/><a:ext cx=\"10058400\" cy=\"4754880\"/></a:xfrm><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom><a:noFill/></p:spPr><p:txBody><a:bodyPr/><a:lstStyle/>" +
                "<a:p><a:r><a:rPr lang=\"en-US\" sz=\"3600\" b=\"1\"><a:solidFill><a:srgbClr val=\"FFFFFF\"/></a:solidFill><a:latin typeface=\"Calibri\"/></a:rPr><a:t>Key Takeaways &amp; System Summary</a:t></a:r></a:p>" +
                buildSummaryBullet("Centralized Platform: Unifies event creation, registration, and attendee management into one portal.") +
                buildSummaryBullet("Servlet API Excellence: Complete suite including REST JSON API, Async SSE streaming, SVG QR badges, and CSV export.") +
                buildSummaryBullet("Direct JDBC & ACID Transactions: Raw PreparedStatements with explicit commit and rollback ensures data integrity.") +
                buildSummaryBullet("High Concurrency & Lock Safety: Per-event monitor locks prevent race conditions; background daemon audits capacity.") +
                buildSummaryBullet("Enterprise Quality & Innovation: Rate limiting security, XSS sanitization, and automated startup stress-testing.") +
                "</p:txBody></p:sp>" +
                "</p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sld>";
    }

    private static String buildSummaryBullet(String text) {
        return "<a:p><a:pPr><a:spcBfr><a:spcPts val=\"1400\"/></a:spcBfr></a:pPr><a:r><a:rPr lang=\"en-US\" sz=\"1800\"><a:solidFill><a:srgbClr val=\"E2E8F0\"/></a:solidFill><a:latin typeface=\"Calibri\"/></a:rPr><a:t>•  " + escape(text) + "</a:t></a:r></a:p>";
    }

    private static String buildTwoCardSlide(String title, String subtitle,
                                            String card1Title, String[] card1Items, String card1Color,
                                            String card2Title, String[] card2Items, String card2Color) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>\n");
        sb.append("<p:sld xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/><a:chOff x=\"0\" y=\"0\"/><a:chExt cx=\"0\" cy=\"0\"/></a:xfrm></p:grpSpPr>");
        sb.append(buildHeader(title, subtitle));

        // Card 1
        sb.append(buildCardXml(3, 731520, 1645920, 5120640, 4389120, card1Title, card1Items, card1Color));
        // Card 2
        sb.append(buildCardXml(4, 6126480, 1645920, 5120640, 4389120, card2Title, card2Items, card2Color));

        sb.append("</p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sld>");
        return sb.toString();
    }

    private static String buildCardXml(int id, long x, long y, long cx, long cy, String cardTitle, String[] items, String titleColor) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p:sp><p:nvSpPr><p:cNvPr id=\"").append(id).append("\" name=\"Card").append(id).append("\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>");
        sb.append("<p:spPr><a:xfrm><a:off x=\"").append(x).append("\" y=\"").append(y).append("\"/><a:ext cx=\"").append(cx).append("\" cy=\"").append(cy).append("\"/></a:xfrm>");
        sb.append("<a:prstGeom prst=\"roundRect\"><a:avLst><a:gd name=\"adj\" fmla=\"val 2000\"/></a:avLst></a:prstGeom>");
        sb.append("<a:solidFill><a:srgbClr val=\"F8FAFC\"/></a:solidFill><a:ln w=\"19050\"><a:solidFill><a:srgbClr val=\"E2E8F0\"/></a:solidFill></a:ln></p:spPr>");
        sb.append("<p:txBody><a:bodyPr lIns=\"274320\" tIns=\"274320\" rIns=\"274320\" bIns=\"274320\"/><a:lstStyle/>");
        sb.append("<a:p><a:r><a:rPr lang=\"en-US\" sz=\"2000\" b=\"1\"><a:solidFill><a:srgbClr val=\"").append(titleColor).append("\"/></a:solidFill><a:latin typeface=\"Calibri\"/></a:rPr><a:t>").append(escape(cardTitle)).append("</a:t></a:r></a:p>");

        for (String item : items) {
            sb.append("<a:p><a:pPr><a:spcBfr><a:spcPts val=\"1200\"/></a:spcBfr></a:pPr><a:r><a:rPr lang=\"en-US\" sz=\"1400\"><a:solidFill><a:srgbClr val=\"334155\"/></a:solidFill><a:latin typeface=\"Calibri\"/></a:rPr><a:t>•  ").append(escape(item)).append("</a:t></a:r></a:p>");
        }

        sb.append("</p:txBody></p:sp>");
        return sb.toString();
    }

    private static String buildCodeSlide(String title, String subtitle, String code, String notes) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>\n");
        sb.append("<p:sld xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/><a:chOff x=\"0\" y=\"0\"/><a:chExt cx=\"0\" cy=\"0\"/></a:xfrm></p:grpSpPr>");
        sb.append(buildHeader(title, subtitle));

        // Code Editor Card (Dark Theme #0F172A)
        sb.append("<p:sp><p:nvSpPr><p:cNvPr id=\"3\" name=\"CodeBox\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>");
        sb.append("<p:spPr><a:xfrm><a:off x=\"731520\" y=\"1645920\"/><a:ext cx=\"6858000\" cy=\"4754880\"/></a:xfrm>");
        sb.append("<a:prstGeom prst=\"roundRect\"><a:avLst><a:gd name=\"adj\" fmla=\"val 1500\"/></a:avLst></a:prstGeom>");
        sb.append("<a:solidFill><a:srgbClr val=\"0F172A\"/></a:solidFill><a:ln w=\"19050\"><a:solidFill><a:srgbClr val=\"334155\"/></a:solidFill></a:ln></p:spPr>");
        sb.append("<p:txBody><a:bodyPr lIns=\"228600\" tIns=\"228600\" rIns=\"228600\" bIns=\"228600\"/><a:lstStyle/>");

        String[] lines = code.split("\n");
        for (String line : lines) {
            if (line.isEmpty()) {
                sb.append("<a:p><a:endParaRPr lang=\"en-US\" sz=\"1100\"/></a:p>");
                continue;
            }
            String color = "F8FAFC"; // default white
            if (line.trim().startsWith("//")) {
                color = "4ADE80"; // green comments
            } else if (line.contains("class") || line.contains("public") || line.contains("throw") || line.contains("return") || line.contains("synchronized") || line.contains("try") || line.contains("catch") || line.contains("finally") || line.contains("if") || line.contains("else") || line.contains("new")) {
                color = "38BDF8"; // blue keyword
            } else if (line.contains("\"")) {
                color = "FBBF24"; // yellow string
            }

            sb.append("<a:p><a:r><a:rPr lang=\"en-US\" sz=\"1100\"><a:solidFill><a:srgbClr val=\"").append(color).append("\"/></a:solidFill><a:latin typeface=\"Consolas\"/></a:rPr><a:t xml:space=\"preserve\">").append(escape(line)).append("</a:t></a:r></a:p>");
        }
        sb.append("</p:txBody></p:sp>");

        // Side Explanation Card
        sb.append("<p:sp><p:nvSpPr><p:cNvPr id=\"4\" name=\"NotesBox\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>");
        sb.append("<p:spPr><a:xfrm><a:off x=\"7863840\" y=\"1645920\"/><a:ext cx=\"3566160\" cy=\"4754880\"/></a:xfrm>");
        sb.append("<a:prstGeom prst=\"roundRect\"><a:avLst><a:gd name=\"adj\" fmla=\"val 2000\"/></a:avLst></a:prstGeom>");
        sb.append("<a:solidFill><a:srgbClr val=\"F8FAFC\"/></a:solidFill><a:ln w=\"19050\"><a:solidFill><a:srgbClr val=\"E2E8F0\"/></a:solidFill></a:ln></p:spPr>");
        sb.append("<p:txBody><a:bodyPr lIns=\"228600\" tIns=\"228600\" rIns=\"228600\" bIns=\"228600\"/><a:lstStyle/>");

        String[] noteLines = notes.split("\n");
        for (String n : noteLines) {
            if (n.isEmpty()) {
                sb.append("<a:p><a:endParaRPr lang=\"en-US\" sz=\"1200\"/></a:p>");
                continue;
            }
            boolean isBold = n.startsWith("Code Highlights:") || n.startsWith("•");
            String color = isBold ? "1E293B" : "64748B";
            int sz = isBold ? 1300 : 1200;
            sb.append("<a:p><a:r><a:rPr lang=\"en-US\" sz=\"").append(sz).append("\" b=\"").append(isBold ? "1" : "0").append("\"><a:solidFill><a:srgbClr val=\"").append(color).append("\"/></a:solidFill><a:latin typeface=\"Calibri\"/></a:rPr><a:t>").append(escape(n)).append("</a:t></a:r></a:p>");
        }
        sb.append("</p:txBody></p:sp>");

        sb.append("</p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sld>");
        return sb.toString();
    }

    private static String buildContentTypes(int totalSlides) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>\n");
        sb.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">");
        sb.append("<Default Extension=\"bin\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.printerSettings\"/>");
        sb.append("<Default Extension=\"jpeg\" ContentType=\"image/jpeg\"/>");
        sb.append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>");
        sb.append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>");
        sb.append("<Override PartName=\"/docProps/app.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.extended-properties+xml\"/>");
        sb.append("<Override PartName=\"/docProps/core.xml\" ContentType=\"application/vnd.openxmlformats-package.core-properties+xml\"/>");
        sb.append("<Override PartName=\"/ppt/presProps.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.presProps+xml\"/>");
        sb.append("<Override PartName=\"/ppt/presentation.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml\"/>");

        for (int i = 1; i <= 11; i++) {
            sb.append("<Override PartName=\"/ppt/slideLayouts/slideLayout").append(i).append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml\"/>");
        }
        sb.append("<Override PartName=\"/ppt/slideMasters/slideMaster1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml\"/>");

        for (int i = 1; i <= totalSlides; i++) {
            sb.append("<Override PartName=\"/ppt/slides/slide").append(i).append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slide+xml\"/>");
        }

        sb.append("<Override PartName=\"/ppt/tableStyles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.tableStyles+xml\"/>");
        sb.append("<Override PartName=\"/ppt/theme/theme1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.theme+xml\"/>");
        sb.append("<Override PartName=\"/ppt/viewProps.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.viewProps+xml\"/>");
        sb.append("</Types>");
        return sb.toString();
    }

    private static String buildPresentationXml(int totalSlides) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>\n");
        sb.append("<p:presentation xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
        sb.append("<p:sldMasterIdLst><p:sldMasterId id=\"2147483648\" r:id=\"rId1\"/></p:sldMasterIdLst>");
        sb.append("<p:sldIdLst>");
        for (int i = 1; i <= totalSlides; i++) {
            sb.append("<p:sldId id=\"").append(255 + i).append("\" r:id=\"rId").append(6 + i).append("\"/>");
        }
        sb.append("</p:sldIdLst>");
        sb.append("<p:sldSz cx=\"12192000\" cy=\"6858000\" type=\"screen16x9\"/><p:notesSz cx=\"6858000\" cy=\"9144000\"/>");
        sb.append("</p:presentation>");
        return sb.toString();
    }

    private static String buildPresentationRels(int totalSlides) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>\n");
        sb.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        sb.append("<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster\" Target=\"slideMasters/slideMaster1.xml\"/>");
        sb.append("<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/printerSettings\" Target=\"printerSettings/printerSettings1.bin\"/>");
        sb.append("<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/presProps\" Target=\"presProps.xml\"/>");
        sb.append("<Relationship Id=\"rId4\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/viewProps\" Target=\"viewProps.xml\"/>");
        sb.append("<Relationship Id=\"rId5\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme\" Target=\"theme/theme1.xml\"/>");
        sb.append("<Relationship Id=\"rId6\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/tableStyles\" Target=\"tableStyles.xml\"/>");

        for (int i = 1; i <= totalSlides; i++) {
            sb.append("<Relationship Id=\"rId").append(6 + i).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide\" Target=\"slides/slide").append(i).append(".xml\"/>");
        }

        sb.append("</Relationships>");
        return sb.toString();
    }
}
