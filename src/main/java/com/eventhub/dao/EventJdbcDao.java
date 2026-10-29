package com.eventhub.dao;

import com.eventhub.model.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class EventJdbcDao {

    // Injected DataSource to supply pooled raw database connections
    private final DataSource dataSource;

    @Autowired
    public EventJdbcDao(DataSource dataSource) {
        // Inject DataSource dependency
        this.dataSource = dataSource;
    }

    public Event save(Event event) {
        // Parameterized SQL insert statement for event records
        String sql = "INSERT INTO events (title, category, description, event_date, venue, capacity, " +
                     "organizer_name, organizer_email, organizer_phone, banner_url, status, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // Acquire raw JDBC connection and request auto-generated primary keys
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Bind title parameter to positional placeholder 1
            ps.setString(1, event.getTitle());
            // Bind category parameter to placeholder 2
            ps.setString(2, event.getCategory());
            // Bind description parameter to placeholder 3
            ps.setString(3, event.getDescription());
            // Convert LocalDateTime to java.sql.Timestamp for SQL DATETIME column
            ps.setTimestamp(4, Timestamp.valueOf(event.getEventDate()));
            // Bind venue parameter to placeholder 5
            ps.setString(5, event.getVenue());
            // Bind maximum attendee capacity to placeholder 6
            ps.setInt(6, event.getCapacity());
            // Bind organizer name to placeholder 7
            ps.setString(7, event.getOrganizerName());
            // Bind organizer email to placeholder 8
            ps.setString(8, event.getOrganizerEmail());
            // Bind optional organizer phone to placeholder 9
            ps.setString(9, event.getOrganizerPhone());
            // Bind optional promotional banner URL to placeholder 10
            ps.setString(10, event.getBannerUrl());
            // Set operational status default to ACTIVE if none was supplied
            ps.setString(11, event.getStatus() != null ? event.getStatus() : "ACTIVE");
            // Set creation audit timestamp to current system time
            ps.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));

            // Execute insert operation on database table
            ps.executeUpdate();

            // Retrieve database-generated auto-increment primary key ID
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    // Populate generated primary key back into the entity object
                    event.setId(generatedKeys.getLong(1));
                }
            }

            return event;
        } catch (SQLException e) {
            // Translate checked SQLException into unchecked runtime exception
            throw new RuntimeException("JDBC save failed for event: " + e.getMessage(), e);
        }
    }

    public Optional<Event> findById(Long id) {
        // Parameterized SQL query selecting single event by primary key
        String sql = "SELECT * FROM events WHERE id = ?";

        // Open database connection and prepare statement with parameter binding
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Bind event ID to positional placeholder 1
            ps.setLong(1, id);

            // Execute SQL query and inspect single returned row
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Map current ResultSet row columns into an Event entity instance
                    return Optional.of(mapRowToEvent(rs));
                }
            }

            // Return empty optional if no record matched the given identifier
            return Optional.empty();
        } catch (SQLException e) {
            // Rethrow database exception with context
            throw new RuntimeException("JDBC findById failed for ID: " + id, e);
        }
    }

    public List<Event> findAll() {
        List<Event> events = new ArrayList<>();
        // SQL query selecting all events ordered by event date chronologically
        String sql = "SELECT * FROM events ORDER BY event_date ASC";

        // Execute query using try-with-resources for automatic resource closing
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Advance cursor row-by-row through the ResultSet
            while (rs.next()) {
                // Map each row into an Event domain object and append to list
                events.add(mapRowToEvent(rs));
            }
        } catch (SQLException e) {
            // Rethrow runtime database failure
            throw new RuntimeException("JDBC findAll failed: " + e.getMessage(), e);
        }

        return events;
    }

    public boolean update(Event event) {
        // Parameterized SQL updating existing event attributes matching ID
        String sql = "UPDATE events SET title = ?, category = ?, description = ?, event_date = ?, " +
                     "venue = ?, capacity = ?, organizer_name = ?, organizer_email = ?, " +
                     "organizer_phone = ?, banner_url = ?, status = ? WHERE id = ?";

        // Prepare update statement
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Bind updated field parameters
            ps.setString(1, event.getTitle());
            ps.setString(2, event.getCategory());
            ps.setString(3, event.getDescription());
            ps.setTimestamp(4, Timestamp.valueOf(event.getEventDate()));
            ps.setString(5, event.getVenue());
            ps.setInt(6, event.getCapacity());
            ps.setString(7, event.getOrganizerName());
            ps.setString(8, event.getOrganizerEmail());
            ps.setString(9, event.getOrganizerPhone());
            ps.setString(10, event.getBannerUrl());
            ps.setString(11, event.getStatus());
            // Bind primary key WHERE criteria
            ps.setLong(12, event.getId());

            // Execute update and return true if at least one row was modified
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Wrap and rethrow SQL exception
            throw new RuntimeException("JDBC update failed for event ID: " + event.getId(), e);
        }
    }

    public boolean delete(Long id) {
        // Parameterized SQL query deleting event row by ID
        String sql = "DELETE FROM events WHERE id = ?";

        // Execute deletion
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Bind primary key identifier to delete
            ps.setLong(1, id);

            // Return true if record was removed
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Rethrow runtime exception
            throw new RuntimeException("JDBC delete failed for event ID: " + id, e);
        }
    }

    private Event mapRowToEvent(ResultSet rs) throws SQLException {
        // Instantiate empty Event domain model
        Event event = new Event();
        // Extract primary key identifier
        event.setId(rs.getLong("id"));
        // Extract title string
        event.setTitle(rs.getString("title"));
        // Extract category string
        event.setCategory(rs.getString("category"));
        // Extract description text
        event.setDescription(rs.getString("description"));
        // Convert SQL Timestamp back into modern Java LocalDateTime
        Timestamp ts = rs.getTimestamp("event_date");
        if (ts != null) {
            event.setEventDate(ts.toLocalDateTime());
        }
        // Extract venue location
        event.setVenue(rs.getString("venue"));
        // Extract seat capacity
        event.setCapacity(rs.getInt("capacity"));
        // Extract organizer name
        event.setOrganizerName(rs.getString("organizer_name"));
        // Extract organizer email
        event.setOrganizerEmail(rs.getString("organizer_email"));
        // Extract organizer phone
        event.setOrganizerPhone(rs.getString("organizer_phone"));
        // Extract banner image URL
        event.setBannerUrl(rs.getString("banner_url"));
        // Extract event status
        event.setStatus(rs.getString("status"));

        return event;
    }
}
