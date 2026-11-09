package com.eventhub.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthService.class);
    public static final String SESSION_ADMIN_KEY = "LOGGED_IN_ADMIN";

    private final PasswordHasher passwordHasher;

    @Value("${app.admin.username:admin}")
    private String configuredUsername = "admin";

    @Value("${app.admin.password:AdminPassword123!}")
    private String configuredInitialPassword = "AdminPassword123!";

    private String storedPasswordHash;

    @Autowired
    public AdminAuthService(PasswordHasher passwordHasher) {
        this.passwordHasher = passwordHasher;
    }

    public AdminAuthService(PasswordHasher passwordHasher, String configuredUsername, String configuredInitialPassword) {
        this.passwordHasher = passwordHasher;
        this.configuredUsername = configuredUsername != null ? configuredUsername : "admin";
        this.configuredInitialPassword = configuredInitialPassword != null ? configuredInitialPassword : "AdminPassword123!";
    }

    @PostConstruct
    public void initializeAdminCredentials() {
        // Hash the configured administrator password at startup using PBKDF2-SHA512
        this.storedPasswordHash = passwordHasher.hashPassword(configuredInitialPassword);
        log.info("[SECURITY] Admin authentication service initialized for username '{}'.", configuredUsername);
    }

    /**
     * Authenticates an admin user and attaches user state to the HTTP session.
     */
    public boolean authenticate(String username, String password, HttpSession session) {
        if (username == null || password == null) {
            return false;
        }

        boolean usernameMatch = configuredUsername.trim().equalsIgnoreCase(username.trim());
        boolean passwordMatch = passwordHasher.verifyPassword(password, storedPasswordHash);

        if (usernameMatch && passwordMatch) {
            session.setAttribute(SESSION_ADMIN_KEY, configuredUsername);
            log.info("[SECURITY] Administrator '{}' logged in successfully from session {}.", configuredUsername, session.getId());
            return true;
        }

        log.warn("[SECURITY] Failed login attempt for username '{}' on session {}.", username, session.getId());
        return false;
    }

    /**
     * Checks if current HTTP session has an authenticated administrator.
     */
    public boolean isAuthenticated(HttpSession session) {
        if (session == null) {
            return false;
        }
        Object adminUser = session.getAttribute(SESSION_ADMIN_KEY);
        return adminUser != null && !adminUser.toString().trim().isEmpty();
    }

    /**
     * Logs out administrator and clears the session.
     */
    public void logout(HttpSession session) {
        if (session != null) {
            log.info("[SECURITY] Admin '{}' logging out.", session.getAttribute(SESSION_ADMIN_KEY));
            session.removeAttribute(SESSION_ADMIN_KEY);
            session.invalidate();
        }
    }

    public String getConfiguredUsername() {
        return configuredUsername;
    }
}
