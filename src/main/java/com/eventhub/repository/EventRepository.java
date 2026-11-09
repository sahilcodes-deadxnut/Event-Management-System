package com.eventhub.repository;

import com.eventhub.model.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    // Retrieve events filtered by lifecycle status (e.g. ACTIVE, CLOSED)
    List<Event> findByStatus(String status);

    // Fetch active events ordered chronologically by scheduled date
    List<Event> findByStatusOrderByEventDateAsc(String status);

    // Retrieve events matching a specific category name (case-insensitive)
    List<Event> findByCategoryIgnoreCase(String category);

    // Filter events by both operational status and category
    List<Event> findByStatusAndCategoryIgnoreCase(String status, String category);

    // Search events containing a keyword substring within their title
    List<Event> findByTitleContainingIgnoreCase(String keyword);

    // Full-text query searching title or description of active events
    @Query("SELECT e FROM Event e WHERE (LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :query, '%'))) AND e.status = 'ACTIVE' ORDER BY e.eventDate ASC")
    List<Event> searchActiveEvents(@Param("query") String query);

    // Composite query matching category and searching title or description of active events
    @Query("SELECT e FROM Event e WHERE LOWER(e.category) = LOWER(:category) AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :query, '%'))) AND e.status = 'ACTIVE' ORDER BY e.eventDate ASC")
    List<Event> searchActiveEventsWithCategory(@Param("query") String query, @Param("category") String category);

    // Query distinct existing categories across all events to populate UI filter dropdowns
    @Query("SELECT DISTINCT e.category FROM Event e ORDER BY e.category ASC")
    List<String> findDistinctCategories();

    // Count how many active events are scheduled to occur in the future
    @Query("SELECT COUNT(e) FROM Event e WHERE e.eventDate > CURRENT_TIMESTAMP AND e.status = 'ACTIVE'")
    Long countUpcomingEvents();

    /**
     * Acquires a database-level exclusive row lock (SELECT ... FOR UPDATE)
     * guaranteeing true mutual exclusion across multiple concurrent transactions
     * and horizontal application instances.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdWithPessimisticLock(@Param("id") Long id);
}
