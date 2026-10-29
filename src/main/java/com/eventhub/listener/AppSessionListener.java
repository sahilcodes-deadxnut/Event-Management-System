package com.eventhub.listener;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AppSessionListener implements HttpSessionListener {

    private static final Logger log = LoggerFactory.getLogger(AppSessionListener.class);

    // Thread-safe atomic counter tracking currently active HTTP user sessions
    private static final AtomicInteger activeSessions = new AtomicInteger(0);

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        // Increment count when a new HTTP session is created by the container
        int currentCount = activeSessions.incrementAndGet();
        log.info("[SESSION CREATED] New user session established. Active sessions count: {}", currentCount);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        // Decrement count when a session times out or is invalidated
        int currentCount = activeSessions.decrementAndGet();
        log.info("[SESSION DESTROYED] User session expired or invalidated. Active sessions count: {}", currentCount);
    }

    public static int getActiveSessionsCount() {
        // Getter for current active sessions metric
        return activeSessions.get();
    }
}
