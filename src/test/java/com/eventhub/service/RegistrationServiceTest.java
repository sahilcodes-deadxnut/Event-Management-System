package com.eventhub.service;

import com.eventhub.concurrency.BookingLockManager;
import com.eventhub.model.Event;
import com.eventhub.model.Registration;
import com.eventhub.repository.EventRepository;
import com.eventhub.repository.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrationServiceTest {

    private RegistrationService registrationService;
    private final Map<Long, Event> eventsMap = new HashMap<>();
    private final Map<Long, Registration> registrationsMap = new HashMap<>();
    private final Set<String> registeredEmails = new HashSet<>();
    private Long lastDeletedRegId = null;

    private Event activeEvent;
    private Registration newRegistration;

    @BeforeEach
    void setUp() {
        eventsMap.clear();
        registrationsMap.clear();
        registeredEmails.clear();
        lastDeletedRegId = null;

        activeEvent = new Event(
                "AI Workshop 2026",
                "Workshop",
                "Hands-on ML lab",
                LocalDateTime.now().plusDays(5),
                "Lab 101",
                2, // capacity = 2
                "Dr. Vance",
                "vance@ai.org",
                "+1-555-0199",
                ""
        );
        activeEvent.setId(10L);
        activeEvent.setStatus("ACTIVE");
        eventsMap.put(10L, activeEvent);

        newRegistration = new Registration();
        newRegistration.setParticipantName("Alice Smith");
        newRegistration.setEmail("Alice.Smith@example.COM");
        newRegistration.setPhone("+1-555-1111");
        newRegistration.setOrganization("Acme Corp");

        EventRepository proxyEventRepo = (EventRepository) Proxy.newProxyInstance(
                EventRepository.class.getClassLoader(),
                new Class<?>[]{EventRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findById".equals(name) || "findByIdWithPessimisticLock".equals(name)) {
                        Long id = (Long) args[0];
                        return Optional.ofNullable(eventsMap.get(id));
                    }
                    return null;
                }
        );

        RegistrationRepository proxyRegRepo = (RegistrationRepository) Proxy.newProxyInstance(
                RegistrationRepository.class.getClassLoader(),
                new Class<?>[]{RegistrationRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("countByEventId".equals(name)) {
                        return (long) registrationsMap.size();
                    } else if ("existsByEventIdAndEmailIgnoreCase".equals(name)) {
                        String email = (String) args[1];
                        return registeredEmails.contains(email.toLowerCase());
                    } else if ("save".equals(name)) {
                        Registration r = (Registration) args[0];
                        if (r.getId() == null) r.setId(100L + registrationsMap.size());
                        registrationsMap.put(r.getId(), r);
                        registeredEmails.add(r.getEmail().toLowerCase());
                        return r;
                    } else if ("findById".equals(name)) {
                        Long id = (Long) args[0];
                        return Optional.ofNullable(registrationsMap.get(id));
                    } else if ("deleteById".equals(name)) {
                        lastDeletedRegId = (Long) args[0];
                        Registration removed = registrationsMap.remove(lastDeletedRegId);
                        if (removed != null) registeredEmails.remove(removed.getEmail().toLowerCase());
                        return null;
                    } else if ("findByRegistrationCode".equals(name)) {
                        String code = (String) args[0];
                        return registrationsMap.values().stream()
                                .filter(r -> code.equalsIgnoreCase(r.getRegistrationCode()))
                                .findFirst();
                    }
                    return null;
                }
        );

        PlatformTransactionManager txManager = new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() { return new Object(); }
            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {}
            @Override
            protected void doCommit(DefaultTransactionStatus status) {}
            @Override
            protected void doRollback(DefaultTransactionStatus status) {}
        };

        registrationService = new RegistrationService(proxyRegRepo, proxyEventRepo, new BookingLockManager(), txManager);
    }

    @Test
    @DisplayName("registerParticipant successfully creates registration with ticket code and normalized email")
    void testSuccessfulRegistration() {
        Registration result = registrationService.registerParticipant(10L, newRegistration);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("alice.smith@example.com"); // Normalized
        assertThat(result.getRegistrationCode()).startsWith("EVT-");
        assertThat(result.getEvent()).isEqualTo(activeEvent);
        assertThat(registrationsMap).containsKey(result.getId());
    }

    @Test
    @DisplayName("registerParticipant rejects duplicate email for the same event")
    void testDuplicateEmailRejection() {
        // Register once
        registrationService.registerParticipant(10L, newRegistration);

        // Attempt duplicate registration with different casing/whitespace
        Registration duplicate = new Registration();
        duplicate.setParticipantName("Alice S.");
        duplicate.setEmail("  ALICE.smith@example.com  ");
        duplicate.setPhone("+1-555-2222");

        assertThatThrownBy(() -> registrationService.registerParticipant(10L, duplicate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("registerParticipant rejects booking when event capacity is reached")
    void testCapacityLimitRejection() {
        // Active event capacity is 2
        Registration reg1 = new Registration();
        reg1.setParticipantName("Attendee 1");
        reg1.setEmail("att1@test.org");
        reg1.setPhone("+1-555-1111");
        registrationService.registerParticipant(10L, reg1);

        Registration reg2 = new Registration();
        reg2.setParticipantName("Attendee 2");
        reg2.setEmail("att2@test.org");
        reg2.setPhone("+1-555-2222");
        registrationService.registerParticipant(10L, reg2);

        // Third registration must be rejected
        Registration reg3 = new Registration();
        reg3.setParticipantName("Attendee 3");
        reg3.setEmail("att3@test.org");
        reg3.setPhone("+1-555-3333");

        assertThatThrownBy(() -> registrationService.registerParticipant(10L, reg3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("maximum capacity");
    }

    @Test
    @DisplayName("registerParticipant rejects booking on closed or cancelled events")
    void testClosedEventRejection() {
        activeEvent.setStatus("CLOSED");

        assertThatThrownBy(() -> registrationService.registerParticipant(10L, newRegistration))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Registration is closed");
    }

    @Test
    @DisplayName("cancelRegistration deletes registration record and releases seat")
    void testCancelRegistration() {
        Registration saved = registrationService.registerParticipant(10L, newRegistration);
        Long id = saved.getId();

        registrationService.cancelRegistration(id);

        assertThat(lastDeletedRegId).isEqualTo(id);
        assertThat(registrationsMap).doesNotContainKey(id);
    }
}
