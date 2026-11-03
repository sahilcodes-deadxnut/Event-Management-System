package com.eventhub.servlet;

import com.eventhub.dao.EventAnalyticsDao;
import com.eventhub.dto.EventOccupancyDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class LiveCapacitySseServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(LiveCapacitySseServlet.class);

    // Thread-safe list maintaining active client AsyncContext listeners
    private final List<AsyncContext> activeContexts = new CopyOnWriteArrayList<>();
    // Injected Direct JDBC Analytics DAO to query real-time capacity numbers
    private final EventAnalyticsDao eventAnalyticsDao;
    private final ObjectMapper objectMapper;
    // Scheduled background executor pushing periodic SSE heartbeats and capacity updates
    private ScheduledExecutorService broadcastExecutor;

    @Autowired
    public LiveCapacitySseServlet(EventAnalyticsDao eventAnalyticsDao, ObjectMapper objectMapper) {
        this.eventAnalyticsDao = eventAnalyticsDao;
        this.objectMapper = objectMapper;
    }

    @Override
    public void init() throws ServletException {
        super.init();
        // Initialize background daemon executor for SSE broadcasts
        broadcastExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LiveCapacitySse-Broadcaster");
            t.setDaemon(true);
            return t;
        });

        // Broadcast live occupancy data every 5 seconds to all connected browsers
        broadcastExecutor.scheduleAtFixedRate(this::broadcastCapacityUpdates, 2, 5, TimeUnit.SECONDS);
        log.info("[SSE SERVLET] LiveCapacitySseServlet initialized with async broadcast executor.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Set standard Server-Sent Events (SSE) headers
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("Access-Control-Allow-Origin", "*");

        // Start Asynchronous Processing on this HTTP request
        AsyncContext asyncContext = request.startAsync(request, response);
        // Set async timeout to 5 minutes
        asyncContext.setTimeout(300_000);

        // Add cleanup listener when client closes browser or connection drops
        asyncContext.addListener(new jakarta.servlet.AsyncListener() {
            @Override
            public void onComplete(jakarta.servlet.AsyncEvent event) {
                activeContexts.remove(asyncContext);
            }

            @Override
            public void onTimeout(jakarta.servlet.AsyncEvent event) {
                activeContexts.remove(asyncContext);
                asyncContext.complete();
            }

            @Override
            public void onError(jakarta.servlet.AsyncEvent event) {
                activeContexts.remove(asyncContext);
            }

            @Override
            public void onStartAsync(jakarta.servlet.AsyncEvent event) {}
        });

        activeContexts.add(asyncContext);

        // Send initial immediate snapshot payload
        try {
            PrintWriter writer = response.getWriter();
            writer.write("event: connected\n");
            writer.write("data: {\"status\":\"connected\",\"listeners\":" + activeContexts.size() + "}\n\n");
            writer.flush();
        } catch (IOException e) {
            activeContexts.remove(asyncContext);
        }
    }

    private void broadcastCapacityUpdates() {
        if (activeContexts.isEmpty()) {
            return;
        }

        try {
            // Query latest stats via Direct JDBC DAO
            List<EventOccupancyDto> analytics = eventAnalyticsDao.getEventOccupancyAnalytics();
            String jsonPayload = objectMapper.writeValueAsString(analytics);

            for (AsyncContext ctx : activeContexts) {
                try {
                    PrintWriter writer = ctx.getResponse().getWriter();
                    writer.write("event: capacity-update\n");
                    writer.write("data: " + jsonPayload + "\n\n");
                    writer.flush();
                } catch (Exception ex) {
                    // Remove disconnected client context
                    activeContexts.remove(ctx);
                    try {
                        ctx.complete();
                    } catch (Exception ignore) {}
                }
            }
        } catch (Exception e) {
            log.error("[SSE ERROR] Error during live capacity broadcast: {}", e.getMessage());
        }
    }

    @Override
    public void destroy() {
        if (broadcastExecutor != null && !broadcastExecutor.isShutdown()) {
            broadcastExecutor.shutdownNow();
        }
        for (AsyncContext ctx : activeContexts) {
            try {
                ctx.complete();
            } catch (Exception ignore) {}
        }
        activeContexts.clear();
        super.destroy();
    }
}
