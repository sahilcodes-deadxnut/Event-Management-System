package com.eventhub.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestExecutionListeners({
        ServletTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class
})
class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET / renders home landing page with statistics and featured events")
    void testHomePageLoads() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("totalEvents"))
                .andExpect(model().attributeExists("featuredEvents"));
    }

    @Test
    @DisplayName("GET /events renders catalog with categories and search controls")
    void testEventsCatalogLoads() throws Exception {
        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(view().name("events"))
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attributeExists("categories"));
    }

    @Test
    @DisplayName("GET /events with category filter executes cleanly")
    void testEventsCatalogCategoryFilter() throws Exception {
        mockMvc.perform(get("/events").param("category", "Workshop"))
                .andExpect(status().isOk())
                .andExpect(view().name("events"))
                .andExpect(model().attributeExists("events"));
    }

    @Test
    @DisplayName("GET /about renders about information page")
    void testAboutPageLoads() throws Exception {
        mockMvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(view().name("about"));
    }

    @Test
    @DisplayName("GET /registrations/lookup loads ticket lookup view")
    void testLookupPageLoads() throws Exception {
        mockMvc.perform(get("/registrations/lookup"))
                .andExpect(status().isOk())
                .andExpect(view().name("lookup"));
    }
}
