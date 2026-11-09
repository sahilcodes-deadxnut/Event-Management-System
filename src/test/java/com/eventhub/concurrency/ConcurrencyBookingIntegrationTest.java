package com.eventhub.concurrency;

import com.eventhub.model.Event;
import com.eventhub.model.Registration;
import com.eventhub.repository.EventRepository;
import com.eventhub.repository.RegistrationRepository;
import com.eventhub.service.RegistrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class
})
class ConcurrencyBookingIntegrationTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private RegistrationService registrationService;

    @Test
    @DisplayName("High concurrency stress test: 10 simultaneous threads competing for 3 seats results in zero overbooking")
    void testConcurrentBookingsCapacityLimit() throws Exception {
        System.out.println(">>> TEST METHOD ENTERED <<<");
        // 1. Create test event with strictly limited capacity = 3 seats
        Event limitedEvent = new Event(
                "Exclusive High-Demand Workshop",
                "Technical",
                "Strict limited seating",
                LocalDateTime.now().plusDays(20),
                "Lab Alpha",
                3, // Only 3 seats available!
                "Organizer Lead",
                "lead@eventhub.test",
                "+1-555-0100",
                ""
        );
        limitedEvent = eventRepository.save(limitedEvent);
        final Long eventId = limitedEvent.getId();

        // 2. Setup 10 concurrent threads contending for the 3 seats
        int totalThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(totalThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 1; i <= totalThreads; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                try {
                    // Wait until all threads are created and ready to fire simultaneously
                    startGate.await();

                    Registration reg = new Registration();
                    reg.setParticipantName("Attendee " + index);
                    reg.setEmail("attendee" + index + "@stress-test.org");
                    reg.setPhone("+1-555-000" + index);

                    registrationService.registerParticipant(eventId, reg);
                    successCount.incrementAndGet();
                } catch (Exception ex) {
                    failureCount.incrementAndGet();
                } finally {
                    finishGate.countDown();
                }
            }));
        }

        // 3. Release gate to fire all threads simultaneously
        startGate.countDown();
        boolean completedInTime = finishGate.await(20, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completedInTime).isTrue();

        // 4. Assert strict concurrency invariants:
        // Exactly 3 succeeded, exactly 7 failed, and DB contains exactly 3 records
        assertThat(successCount.get()).isEqualTo(3);
        assertThat(failureCount.get()).isEqualTo(7);

        long actualInDb = registrationRepository.countByEventId(eventId);
        assertThat(actualInDb).isEqualTo(3);

        // 5. Cleanup
        registrationRepository.deleteAll(registrationRepository.findByEventIdOrderByRegistrationDateDesc(eventId));
        eventRepository.deleteById(eventId);
    }
}
