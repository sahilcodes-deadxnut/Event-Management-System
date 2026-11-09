package com.eventhub.controller;

import com.eventhub.security.AdminAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestExecutionListeners({
        ServletTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class
})
class AdminSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Unauthenticated request to /admin redirects to /admin/login with redirect parameter")
    void testUnauthenticatedAdminAccessRedirects() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/login?redirect=*"));
    }

    @Test
    @DisplayName("Valid admin credentials authenticate successfully and establish session")
    void testSuccessfulAdminLogin() throws Exception {
        mockMvc.perform(post("/admin/login")
                        .param("username", "admin")
                        .param("password", "AdminPassword123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("Invalid admin credentials are rejected with flash error message")
    void testFailedAdminLogin() throws Exception {
        mockMvc.perform(post("/admin/login")
                        .param("username", "admin")
                        .param("password", "IncorrectPassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("Authenticated session can access /admin dashboard successfully")
    void testAuthenticatedAdminAccess() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AdminAuthService.SESSION_ADMIN_KEY, "admin");

        mockMvc.perform(get("/admin").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attributeExists("adminUser"));
    }

    @Test
    @DisplayName("Unauthenticated request to /admin/events/new redirects to login")
    void testUnauthenticatedCreateEventAccessRedirects() throws Exception {
        mockMvc.perform(get("/admin/events/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/login?redirect=*"));
    }

    @Test
    @DisplayName("Admin logout clears session and redirects to login")
    void testAdminLogout() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AdminAuthService.SESSION_ADMIN_KEY, "admin");

        mockMvc.perform(get("/admin/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?logout=true"));
    }
}
