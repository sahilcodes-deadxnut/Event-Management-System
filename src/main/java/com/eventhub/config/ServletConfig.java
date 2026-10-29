package com.eventhub.config;

import com.eventhub.servlet.ParticipantCsvExportServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServletConfig {

    @Bean
    public ServletRegistrationBean<ParticipantCsvExportServlet> participantCsvServletRegistration(ParticipantCsvExportServlet servlet) {
        // Register the standard HttpServlet into the embedded servlet container mapped to '/export/participants.csv'
        ServletRegistrationBean<ParticipantCsvExportServlet> registration =
                new ServletRegistrationBean<>(servlet, "/export/participants.csv");

        // Set servlet initialization order to load immediately upon application startup
        registration.setLoadOnStartup(1);

        // Assign human-readable servlet registration name
        registration.setName("ParticipantCsvExportServlet");

        // Return configured servlet registration bean
        return registration;
    }
}
