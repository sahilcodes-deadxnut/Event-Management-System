package com.eventhub.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingSecurityFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingSecurityFilter.class);

    // Maximum allowed requests per IP address within sliding window (60 requests / minute)
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    // Map tracking client IP addresses and their request count buckets
    private final Map<String, RequestBucket> clientBuckets = new ConcurrentHashMap<>();

    private static class RequestBucket {
        long windowStartTime;
        AtomicInteger count;

        RequestBucket(long now) {
            this.windowStartTime = now;
            this.count = new AtomicInteger(1);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("[RATE LIMIT FILTER] RateLimitingSecurityFilter initialized (Max {} req/min per IP).", MAX_REQUESTS_PER_MINUTE);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip static asset files (.css, .js, .png, .jpg) from rate limiting
        String uri = httpRequest.getRequestURI();
        if (uri.endsWith(".css") || uri.endsWith(".js") || uri.endsWith(".png") || uri.endsWith(".ico")) {
            chain.doFilter(request, response);
            return;
        }

        // Determine client IP (considering X-Forwarded-For if behind reverse proxy)
        String clientIp = httpRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = httpRequest.getRemoteAddr();
        }

        long now = System.currentTimeMillis();
        // Compute or update request bucket for this client IP
        RequestBucket bucket = clientBuckets.compute(clientIp, (ip, existing) -> {
            if (existing == null || now - existing.windowStartTime > 60_000) {
                return new RequestBucket(now);
            } else {
                existing.count.incrementAndGet();
                return existing;
            }
        });

        // Enforce rate limit
        if (bucket.count.get() > MAX_REQUESTS_PER_MINUTE) {
            log.warn("[RATE LIMIT EXCEEDED] IP {} exceeded limit: {} requests in 60s.", clientIp, bucket.count.get());
            httpResponse.setStatus(429); // 429 Too Many Requests
            httpResponse.setContentType("application/json; charset=UTF-8");
            httpResponse.setHeader("Retry-After", "60");
            httpResponse.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please wait 60 seconds before retrying.\"}");
            return;
        }

        // Pass request down the chain
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        clientBuckets.clear();
    }
}
