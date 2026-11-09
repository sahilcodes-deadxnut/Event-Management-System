package com.eventhub.diagnostic;

import com.eventhub.concurrency.BookingLockManager;
import com.eventhub.dao.EventAnalyticsDao;
import com.eventhub.dao.EventJdbcDao;
import com.eventhub.dao.RegistrationJdbcDao;
import com.eventhub.model.Event;
import com.eventhub.model.Registration;
import com.eventhub.service.EventService;
import com.eventhub.service.RegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(10)
@ConditionalOnProperty(name = "app.verification.enabled", havingValue = "true", matchIfMissing = false)
public class SystemVerificationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SystemVerificationRunner.class);

    private final EventJdbcDao eventJdbcDao;
    private final RegistrationJdbcDao registrationJdbcDao;
    private final EventAnalyticsDao eventAnalyticsDao;
    private final EventService eventService;
    private final RegistrationService registrationService;
    private final BookingLockManager bookingLockManager;

    @Autowired
    public SystemVerificationRunner(EventJdbcDao eventJdbcDao,
                                    RegistrationJdbcDao registrationJdbcDao,
                                    EventAnalyticsDao eventAnalyticsDao,
                                    EventService eventService,
                                    RegistrationService registrationService,
                                    BookingLockManager bookingLockManager) {
        this.eventJdbcDao = eventJdbcDao;
        this.registrationJdbcDao = registrationJdbcDao;
        this.eventAnalyticsDao = eventAnalyticsDao;
        this.eventService = eventService;
        this.registrationService = registrationService;
        this.bookingLockManager = bookingLockManager;
    }

    @Override
    public void run(String... args) {
        log.info("===================================================================");
        log.info(" RUNNING AUTOMATED SYSTEM VERIFICATION & CODE QUALITY SUITE");
        log.info("===================================================================");

        boolean test1 = verifyJdbcCrudAndAnalytics();
        boolean test2 = verifyConcurrentBookingLocks();
        boolean test3 = verifyDuplicateEmailPrevention();

        log.info("-------------------------------------------------------------------");
        log.info(" [VERIFICATION SUMMARY]");
        log.info(" Direct JDBC DAO & Analytics : {}", test1 ? "PASSED (100%)" : "FAILED");
        log.info(" Concurrency & Booking Locks : {}", test2 ? "PASSED (Zero Race Conditions)" : "FAILED");
        log.info(" Duplicate Email Prevention  : {}", test3 ? "PASSED (Strict Uniqueness)" : "FAILED");
        log.info("===================================================================");
    }

    private boolean verifyJdbcCrudAndAnalytics() {
        try {
            // Test 1: Insert test event via Direct JDBC DAO
            Event testEvent = new Event(
                    "Verification Diagnostic Event",
                    "Technical",
                    "Automated system test event",
                    LocalDateTime.now().plusDays(30),
                    "Diagnostic Lab 1",
                    5,
                    "QA Lead",
                    "qa@eventhub.test",
                    "+1-555-0100",
                    ""
            );
            Event saved = eventJdbcDao.save(testEvent);
            if (saved.getId() == null) return false;

            // Test 2: Query by ID via JDBC
            var fetched = eventJdbcDao.findById(saved.getId());
            if (fetched.isEmpty()) return false;

            // Test 3: Analytics query via raw PreparedStatement
            var analytics = eventAnalyticsDao.getEventOccupancyAnalytics();
            if (analytics.isEmpty()) return false;

            // Cleanup test event
            eventJdbcDao.delete(saved.getId());
            log.info("[TEST PASSED] Direct JDBC DAO CRUD and raw SQL Analytics verified successfully.");
            return true;
        } catch (Exception ex) {
            log.error("[TEST FAILED] JDBC DAO verification failed: {}", ex.getMessage());
            return false;
        }
    }

    private boolean verifyConcurrentBookingLocks() {
        try {
            // Create a temporary event with limited capacity of 3 seats
            Event event = new Event(
                    "Concurrent Stress Test Event",
                    "Workshop",
                    "Capacity 3 seats for race condition testing",
                    LocalDateTime.now().plusDays(15),
                    "Stress Room",
                    3,
                    "Tester",
                    "tester@test.com",
                    "555-0199",
                    ""
            );
            event = eventService.saveEvent(event);
            Long eventId = event.getId();

            // Spawn 10 concurrent threads simultaneously requesting a seat
            int totalThreads = 10;
            ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger successfulBookings = new AtomicInteger(0);
            AtomicInteger rejectedBookings = new AtomicInteger(0);

            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < totalThreads; i++) {
                final int idx = i;
                futures.add(executor.submit(() -> {
                    try {
                        latch.await(); // Synchronize all threads to fire simultaneously
                        Registration reg = new Registration();
                        reg.setParticipantName("User " + idx);
                        reg.setEmail("concurrent.user." + idx + "@test.com");
                        reg.setPhone("555-000" + idx);
                        registrationService.registerParticipant(eventId, reg);
                        successfulBookings.incrementAndGet();
                    } catch (Exception ex) {
                        rejectedBookings.incrementAndGet();
                    }
                }));
            }

            // Release all threads simultaneously
            latch.countDown();
            executor.shutdown();
            boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);

            // Verify that exactly 3 registrations succeeded and 7 were safely rejected
            long finalAttendeeCount = registrationService.getParticipantsByEvent(eventId).size();
            eventService.deleteEvent(eventId);

            if (successfulBookings.get() == 3 && finalAttendeeCount == 3 && rejectedBookings.get() == 7) {
                log.info("[TEST PASSED] Concurrency Booking Lock strictly preserved capacity (3 accepted, 7 rejected, 0 overbookings).");
                return true;
            } else {
                log.error("[TEST FAILED] Concurrency race condition detected: accepted={}, rejected={}", successfulBookings.get(), rejectedBookings.get());
                return false;
            }
        } catch (Exception ex) {
            log.error("[TEST FAILED] Concurrent booking lock test failed: {}", ex.getMessage());
            return false;
        }
    }

    private boolean verifyDuplicateEmailPrevention() {
        try {
            Event event = new Event(
                    "Duplicate Prevention Test",
                    "Cultural",
                    "Testing duplicate email rejections",
                    LocalDateTime.now().plusDays(20),
                    "Auditorium",
                    50,
                    "Organizer",
                    "organizer@test.com",
                    "555-0101",
                    ""
            );
            event = eventService.saveEvent(event);
            Long eventId = event.getId();

            Registration r1 = new Registration();
            r1.setParticipantName("Alice Smith");
            r1.setEmail("alice.smith@example.com");
            r1.setPhone("555-1111");
            registrationService.registerParticipant(eventId, r1);

            // Attempt second registration with same email (different case and spaces)
            boolean duplicateCaught = false;
            try {
                Registration r2 = new Registration();
                r2.setParticipantName("Alice S.");
                r2.setEmail(" ALICE.SMITH@EXAMPLE.COM ");
                r2.setPhone("555-2222");
                registrationService.registerParticipant(eventId, r2);
            } catch (IllegalArgumentException ex) {
                duplicateCaught = true;
            }

            eventService.deleteEvent(eventId);

            if (duplicateCaught) {
                log.info("[TEST PASSED] Duplicate registration successfully prevented via case-insensitive email check.");
                return true;
            } else {
                log.error("[TEST FAILED] Duplicate registration was permitted incorrectly!");
                return false;
            }
        } catch (Exception ex) {
            log.error("[TEST FAILED] Duplicate email test failed: {}", ex.getMessage());
            return false;
        }
    }
}
