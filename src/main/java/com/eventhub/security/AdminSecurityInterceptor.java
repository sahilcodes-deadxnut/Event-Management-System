package com.eventhub.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class AdminSecurityInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AdminSecurityInterceptor.class);

    private final AdminAuthService adminAuthService;

    public AdminSecurityInterceptor(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // Allow public access to login page and authentication submissions
        if (uri.equals("/admin/login") || uri.startsWith("/admin/login/")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (adminAuthService.isAuthenticated(session)) {
            return true;
        }

        // Unauthenticated access attempt to protected admin route
        log.warn("[ACCESS DENIED] Unauthenticated attempt to access privileged endpoint: {} from IP {}",
                uri, request.getRemoteAddr());

        String target = URLEncoder.encode(uri, StandardCharsets.UTF_8);
        response.sendRedirect("/admin/login?redirect=" + target);
        return false;
    }
}
