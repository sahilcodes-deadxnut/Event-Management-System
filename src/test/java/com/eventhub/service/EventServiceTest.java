package com.eventhub.service;

import com.eventhub.model.Event;
import com.eventhub.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class EventServiceTest {

    private EventService eventService;
    private final Map<Long, Event> storedEvents = new HashMap<>();
    private Long lastDeletedId = null;

    private Event sampleEvent;

    @BeforeEach
    void setUp() {
        storedEvents.clear();
        lastDeletedId = null;

        sampleEvent = new Event(
                "Cloud Computing Summit",
                "Technical",
                "Enterprise cloud best practices",
                LocalDateTime.now().plusDays(10),
                "Hall B",
                100,
                "Alex Taylor",
                "alex@cloudsummit.org",
                "+1-555-0199",
                ""
        );
        sampleEvent.setId(1L);
        storedEvents.put(1L, sampleEvent);

        // Dynamic Proxy simulating EventRepository without heavy bytecode-instrumentation agents
        EventRepository proxyRepo = (EventRepository) Proxy.newProxyInstance(
                EventRepository.class.getClassLoader(),
                new Class<?>[]{EventRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByStatusOrderByEventDateAsc".equals(name)) {
                        return new ArrayList<>(storedEvents.values());
                    } else if ("findById".equals(name)) {
                        Long id = (Long) args[0];
                        return Optional.ofNullable(storedEvents.get(id));
                    } else if ("save".equals(name)) {
                        Event e = (Event) args[0];
                        if (e.getId() == null) e.setId(10L);
                        storedEvents.put(e.getId(), e);
                        return e;
                    } else if ("deleteById".equals(name)) {
                        lastDeletedId = (Long) args[0];
                        storedEvents.remove(lastDeletedId);
                        return null;
                    } else if ("searchActiveEventsWithCategory".equals(name)) {
                        return List.of(sampleEvent);
                    } else if ("findAll".equals(name)) {
                        return new ArrayList<>(storedEvents.values());
                    } else if ("count".equals(name)) {
                        return (long) storedEvents.size();
                    }
                    return null;
                }
        );

        eventService = new EventService(proxyRepo);
    }

    @Test
    @DisplayName("getActiveEvents returns list of active events from repository")
    void testGetActiveEvents() {
        List<Event> result = eventService.getActiveEvents();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Cloud Computing Summit");
    }

    @Test
    @DisplayName("getEventById returns matching event when present")
    void testGetEventById() {
        Optional<Event> found = eventService.getEventById(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(1L);
        assertThat(found.get().getCapacity()).isEqualTo(100);
    }

    @Test
    @DisplayName("saveEvent defaults status to ACTIVE if null or empty")
    void testSaveEventDefaultsStatus() {
        sampleEvent.setStatus(null);

        Event saved = eventService.saveEvent(sampleEvent);

        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("searchAndFilterEvents filters by query and category correctly")
    void testSearchAndFilterEvents() {
        List<Event> result = eventService.searchAndFilterEvents("Cloud", "Technical");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Cloud Computing Summit");
    }

    @Test
    @DisplayName("deleteEvent calls repository deleteById")
    void testDeleteEvent() {
        eventService.deleteEvent(1L);

        assertThat(lastDeletedId).isEqualTo(1L);
        assertThat(storedEvents).doesNotContainKey(1L);
    }
}
