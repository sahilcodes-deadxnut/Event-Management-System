package com.eventhub.service;

import com.eventhub.concurrency.BookingLockManager;
import com.eventhub.model.Event;
import com.eventhub.model.Registration;
import com.eventhub.repository.EventRepository;
import com.eventhub.repository.RegistrationRepository;
import com.eventhub.util.SecuritySanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final BookingLockManager bookingLockManager;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public RegistrationService(RegistrationRepository registrationRepository,
                               EventRepository eventRepository,
                               BookingLockManager bookingLockManager,
                               PlatformTransactionManager transactionManager) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.bookingLockManager = bookingLockManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Atomically registers a participant for an event.
     * Synchronizes on a per-event lock monitor that wraps the transaction boundary,
     * guaranteeing the database transaction commits completely BEFORE releasing the lock.
     * Prevents last-seat race conditions and capacity overbooking under heavy concurrent load.
     */
    public Registration registerParticipant(Long eventId, Registration registration) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null.");
        }
        if (registration == null) {
            throw new IllegalArgumentException("Registration payload cannot be null.");
        }

        Object eventLock = bookingLockManager.getLock(eventId);
        synchronized (eventLock) {
            return transactionTemplate.execute(status -> {
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));

                if (!"ACTIVE".equalsIgnoreCase(event.getStatus())) {
                    throw new IllegalStateException("Registration is closed. This event is not active (status: " + event.getStatus() + ").");
                }

                long currentCount = registrationRepository.countByEventId(eventId);
                if (currentCount >= event.getCapacity()) {
                    throw new IllegalStateException("Event has reached maximum capacity (" + event.getCapacity() + " seats).");
                }

                // Normalize email: trim, strip control chars, lowercase
                String normalizedEmail = SecuritySanitizer.normalizeEmail(registration.getEmail());
                if (normalizedEmail.isEmpty()) {
                    throw new IllegalArgumentException("Email address is required.");
                }

                if (registrationRepository.existsByEventIdAndEmailIgnoreCase(eventId, normalizedEmail)) {
                    throw new IllegalArgumentException("This email address is already registered for this event: " + normalizedEmail);
                }

                registration.setEvent(event);
                registration.setEmail(normalizedEmail);
                registration.setParticipantName(SecuritySanitizer.sanitize(registration.getParticipantName()));
                registration.setPhone(SecuritySanitizer.sanitize(registration.getPhone()));
                registration.setOrganization(SecuritySanitizer.sanitize(registration.getOrganization()));
                registration.setRoleTitle(SecuritySanitizer.sanitize(registration.getRoleTitle()));
                registration.setSpecialNotes(SecuritySanitizer.sanitize(registration.getSpecialNotes()));
                registration.setRegistrationDate(LocalDateTime.now());

                // Structured collision-resistant admission code
                String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
                int currentYear = LocalDateTime.now().getYear();
                registration.setRegistrationCode("EVT-" + currentYear + "-" + uniqueSuffix);

                Registration saved = registrationRepository.save(registration);
                log.info("[BOOKING SUCCESS] Registered attendee '{}' for event ID {} (Code: {}). Occupancy now: {}/{}",
                        normalizedEmail, eventId, saved.getRegistrationCode(), currentCount + 1, event.getCapacity());
                return saved;
            });
        }
    }

    @Transactional(readOnly = true)
    public List<Registration> getParticipantsByEvent(Long eventId) {
        return registrationRepository.findByEventIdOrderByRegistrationDateDesc(eventId);
    }

    @Transactional(readOnly = true)
    public Optional<Registration> getRegistrationById(Long id) {
        return registrationRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Registration> getRegistrationByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return registrationRepository.findByRegistrationCode(code.trim().toUpperCase());
    }

    public void cancelRegistration(Long id) {
        transactionTemplate.executeWithoutResult(status -> {
            Registration reg = registrationRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Registration not found with ID: " + id));
            Long eventId = reg.getEvent() != null ? reg.getEvent().getId() : null;
            String email = reg.getEmail();
            registrationRepository.deleteById(id);
            log.info("[BOOKING CANCELLED] Cancelled registration ID {} for attendee '{}' on event ID {}. Seat released.",
                    id, email, eventId);
        });
    }

    @Transactional(readOnly = true)
    public long getTotalRegistrationsCount() {
        return registrationRepository.count();
    }
}
