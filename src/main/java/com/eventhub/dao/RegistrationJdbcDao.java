package com.eventhub.dao;

import com.eventhub.model.Event;
import com.eventhub.model.Registration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RegistrationJdbcDao {

    // Injected DataSource providing pooled database connections
    private final DataSource dataSource;
    // JDBC DAO for Event entity retrieval
    private final EventJdbcDao eventJdbcDao;

    @Autowired
    public RegistrationJdbcDao(DataSource dataSource, EventJdbcDao eventJdbcDao) {
        // Inject DataSource and EventJdbcDao dependencies
        this.dataSource = dataSource;
        this.eventJdbcDao = eventJdbcDao;
    }

    public Registration registerParticipantTransactional(Long eventId, Registration registration) {
        // SQL queries executed within a single ACID transaction boundary
        String checkEventSql = "SELECT title, status, capacity FROM events WHERE id = ?";
        String countRegistrationsSql = "SELECT COUNT(*) FROM registrations WHERE event_id = ?";
        String checkDuplicateSql = "SELECT COUNT(*) FROM registrations WHERE event_id = ? AND LOWER(email) = LOWER(?)";
        String insertSql = "INSERT INTO registrations (registration_code, event_id, participant_name, email, " +
                           "phone, organization, role_title, special_notes, registration_date) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            // Obtain raw physical database connection
            conn = dataSource.getConnection();
            // Disable auto-commit to begin a manual transaction
            conn.setAutoCommit(false);

            // Step 1: Verify event exists and is active
            int capacity;
            String eventTitle;
            try (PreparedStatement psEvent = conn.prepareStatement(checkEventSql)) {
                // Bind event ID to query
                psEvent.setLong(1, eventId);
                try (ResultSet rs = psEvent.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Event not found with ID: " + eventId);
                    }
                    String status = rs.getString("status");
                    if (!"ACTIVE".equalsIgnoreCase(status)) {
                        throw new IllegalStateException("Event is not open for registration (status: " + status + ").");
                    }
                    eventTitle = rs.getString("title");
                    capacity = rs.getInt("capacity");
                }
            }

            // Step 2: Verify current registration count against capacity
            try (PreparedStatement psCount = conn.prepareStatement(countRegistrationsSql)) {
                psCount.setLong(1, eventId);
                try (ResultSet rs = psCount.executeQuery()) {
                    if (rs.next() && rs.getInt(1) >= capacity) {
                        throw new IllegalStateException("Event has reached maximum capacity (" + capacity + ").");
                    }
                }
            }

            // Step 3: Prevent duplicate registration by checking if email already exists for this event
            String trimmedEmail = registration.getEmail().trim().toLowerCase();
            try (PreparedStatement psDup = conn.prepareStatement(checkDuplicateSql)) {
                psDup.setLong(1, eventId);
                psDup.setString(2, trimmedEmail);
                try (ResultSet rs = psDup.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new IllegalArgumentException("This email address is already registered for this event: " + trimmedEmail);
                    }
                }
            }

            // Generate unique human-readable ticket code e.g. EVT-2026-A1B2C
            String uniqueSuffix = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
            int currentYear = LocalDateTime.now().getYear();
            String ticketCode = "EVT-" + currentYear + "-" + uniqueSuffix;

            // Step 4: Insert participant registration record
            try (PreparedStatement psInsert = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                psInsert.setString(1, ticketCode);
                psInsert.setLong(2, eventId);
                psInsert.setString(3, registration.getParticipantName());
                psInsert.setString(4, trimmedEmail);
                psInsert.setString(5, registration.getPhone());
                psInsert.setString(6, registration.getOrganization());
                psInsert.setString(7, registration.getRoleTitle());
                psInsert.setString(8, registration.getSpecialNotes());
                psInsert.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));

                // Execute SQL insert statement
                psInsert.executeUpdate();

                // Retrieve auto-generated primary key ID
                try (ResultSet keys = psInsert.getGeneratedKeys()) {
                    if (keys.next()) {
                        registration.setId(keys.getLong(1));
                    }
                }
            }

            // Commit all statements atomically in the transaction
            conn.commit();

            // Populate confirmed properties on registration domain object
            registration.setRegistrationCode(ticketCode);
            registration.setEmail(trimmedEmail);
            registration.setRegistrationDate(LocalDateTime.now());

            // Attach parent event reference
            Event event = new Event();
            event.setId(eventId);
            event.setTitle(eventTitle);
            registration.setEvent(event);

            return registration;

        } catch (Exception ex) {
            // Roll back all transactional changes if any error or constraint violation occurred
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    // Suppress or log rollback failure
                }
            }
            // Propagate exception to calling layer
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new RuntimeException("Transactional registration failed: " + ex.getMessage(), ex);
        } finally {
            // Reset connection auto-commit mode and close connection back to pool
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    // Suppress connection closing error
                }
            }
        }
    }

    public List<Registration> findByEventId(Long eventId) {
        List<Registration> participants = new ArrayList<>();
        // Query registrations for an event ordered by newest first
        String sql = "SELECT * FROM registrations WHERE event_id = ? ORDER BY registration_date DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Bind eventId parameter
            ps.setLong(1, eventId);

            // Execute query and iterate through matched attendee rows
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Map current row into a Registration model
                    participants.add(mapRowToRegistration(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("JDBC findByEventId failed for event ID: " + eventId, e);
        }

        return participants;
    }

    public Optional<Registration> findByCode(String code) {
        // Query registration by ticket pass code
        String sql = "SELECT * FROM registrations WHERE registration_code = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Bind uppercase code
            ps.setString(1, code.trim().toUpperCase());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToRegistration(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("JDBC findByCode failed for code: " + code, e);
        }
    }

    public boolean cancelRegistration(Long id) {
        // Query deleting participant registration by ID
        String sql = "DELETE FROM registrations WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Bind registration primary key ID
            ps.setLong(1, id);

            // Return true if record was removed
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC cancelRegistration failed for ID: " + id, e);
        }
    }

    private Registration mapRowToRegistration(ResultSet rs) throws SQLException {
        Registration reg = new Registration();
        // Extract primary key identifier
        reg.setId(rs.getLong("id"));
        // Extract registration ticket pass code
        reg.setRegistrationCode(rs.getString("registration_code"));
        // Extract participant full name
        reg.setParticipantName(rs.getString("participant_name"));
        // Extract email address
        reg.setEmail(rs.getString("email"));
        // Extract phone number
        reg.setPhone(rs.getString("phone"));
        // Extract organization affiliation
        reg.setOrganization(rs.getString("organization"));
        // Extract role title
        reg.setRoleTitle(rs.getString("role_title"));
        // Extract special notes
        reg.setSpecialNotes(rs.getString("special_notes"));
        // Extract registration timestamp
        Timestamp ts = rs.getTimestamp("registration_date");
        if (ts != null) {
            reg.setRegistrationDate(ts.toLocalDateTime());
        }

        // Attach parent Event reference
        Long eventId = rs.getLong("event_id");
        eventJdbcDao.findById(eventId).ifPresent(reg::setEvent);

        return reg;
    }
}
