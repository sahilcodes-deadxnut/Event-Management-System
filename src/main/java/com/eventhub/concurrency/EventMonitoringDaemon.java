package com.eventhub.concurrency;

import com.eventhub.model.Event;
import com.eventhub.repository.EventRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(name = "app.daemon.enabled", havingValue = "true", matchIfMissing = true)
public class EventMonitoringDaemon {

    private static final Logger log = LoggerFactory.getLogger(EventMonitoringDaemon.class);

    // Event repository for querying active event data during background audit scans
    private final EventRepository eventRepository;
    // Atomic flag controlling the lifecycle of the daemon background loop
    private final AtomicBoolean running = new AtomicBoolean(false);
    // Reference to background daemon worker thread
    private Thread workerThread;

    @Autowired
    public EventMonitoringDaemon(EventRepository eventRepository) {
        // Inject repository dependency
        this.eventRepository = eventRepository;
    }

    @PostConstruct
    public void startDaemon() {
        // Mark the running state to true before starting thread execution
        running.set(true);

        // Instantiate worker thread executing the background monitoring task
        workerThread = new Thread(this::runMonitoringLoop, "EventMonitor-Daemon-Thread");
        // Mark thread as a daemon so it will not block JVM shutdown
        workerThread.setDaemon(true);
        // Set lower thread priority so background audits do not starve HTTP request processing
        workerThread.setPriority(Thread.MIN_PRIORITY);
        // Start execution of the daemon thread
        workerThread.start();

        log.info("[DAEMON] Event monitoring daemon worker started successfully.");
    }

    private void runMonitoringLoop() {
        // Continue monitoring loop as long as the application remains active
        while (running.get()) {
            try {
                // Sleep for 60 seconds between consecutive monitoring audit sweeps
                Thread.sleep(60_000);

                // Fetch current list of active events to inspect capacity health
                List<Event> activeEvents = eventRepository.findByStatus("ACTIVE");

                // Audit each active event to detect full capacity or high occupancy
                for (Event event : activeEvents) {
                    int remaining = event.getSeatsLeft();
                    int fillPercent = event.getFillPercentage();

                    // Emit log warning if event has reached 90% capacity or is completely sold out
                    if (fillPercent >= 90) {
                        log.warn("[DAEMON AUDIT] High Demand Alert: Event '{}' (ID: {}) is at {}% capacity with only {} seats remaining.",
                                event.getTitle(), event.getId(), fillPercent, remaining);
                    }
                }
            } catch (InterruptedException e) {
                // Restore interrupted status and break the loop on thread shutdown
                Thread.currentThread().interrupt();
                log.info("[DAEMON] Event monitoring daemon received interrupt signal; terminating.");
                break;
            } catch (Exception ex) {
                // Catch unexpected runtime errors to prevent the daemon thread from dying prematurely
                log.error("[DAEMON ERROR] Unexpected error in monitoring sweep: {}", ex.getMessage());
            }
        }
    }

    @PreDestroy
    public void stopDaemon() {
        // Signal the loop to terminate
        running.set(false);
        // Interrupt worker thread if it is currently sleeping
        if (workerThread != null) {
            workerThread.interrupt();
        }
        log.info("[DAEMON] Event monitoring daemon shutdown completed.");
    }
}
