package com.eventhub.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RequestAuditAndEncodingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestAuditAndEncodingFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Lifecycle callback invoked once when the servlet container initializes the filter
        log.info("[FILTER INIT] RequestAuditAndEncodingFilter initialized.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // Cast generic ServletRequest and ServletResponse to HTTP-specific types
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Force UTF-8 character encoding on incoming request parameters and outgoing response
        httpRequest.setCharacterEncoding("UTF-8");
        httpResponse.setCharacterEncoding("UTF-8");

        // Record start timestamp in milliseconds to measure request processing duration
        long startTime = System.currentTimeMillis();

        // Extract HTTP request metadata
        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();
        String clientIp = httpRequest.getRemoteAddr();

        try {
            // Pass the request along the filter chain to the target servlet or controller
            chain.doFilter(request, response);
        } finally {
            // Calculate total execution duration
            long duration = System.currentTimeMillis() - startTime;
            int status = httpResponse.getStatus();

            // Log formatted web audit trail entry
            log.info("[WEB AUDIT] {} {} - Status: {} - Client IP: {} - Time: {}ms",
                    method, uri, status, clientIp, duration);
        }
    }

    @Override
    public void destroy() {
        // Lifecycle callback invoked when the filter is taken out of service
        log.info("[FILTER DESTROY] RequestAuditAndEncodingFilter destroyed.");
    }
}
