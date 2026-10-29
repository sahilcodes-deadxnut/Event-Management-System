package com.eventhub.config;

import com.eventhub.filter.RequestAuditAndEncodingFilter;
import com.eventhub.listener.AppSessionListener;
import com.eventhub.servlet.EventListServlet;
import com.eventhub.servlet.EventRegistrationServlet;
import com.eventhub.servlet.ParticipantCsvExportServlet;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServletConfig {

    @Bean
    public ServletRegistrationBean<ParticipantCsvExportServlet> participantCsvServletRegistration(
            ParticipantCsvExportServlet servlet) {
        // Register the CSV export HttpServlet mapped to '/export/participants.csv'
        ServletRegistrationBean<ParticipantCsvExportServlet> registration =
                new ServletRegistrationBean<>(servlet, "/export/participants.csv");
        // Load servlet immediately on container startup
        registration.setLoadOnStartup(1);
        registration.setName("ParticipantCsvExportServlet");
        return registration;
    }

    @Bean
    public ServletRegistrationBean<EventListServlet> eventListServletRegistration(
            EventListServlet servlet) {
        // Register the EventListServlet mapped to '/servlets/events'
        ServletRegistrationBean<EventListServlet> registration =
                new ServletRegistrationBean<>(servlet, "/servlets/events");
        registration.setLoadOnStartup(2);
        registration.setName("EventListServlet");
        return registration;
    }

    @Bean
    public ServletRegistrationBean<EventRegistrationServlet> eventRegistrationServletRegistration(
            EventRegistrationServlet servlet) {
        // Register the EventRegistrationServlet mapped to '/servlets/register'
        ServletRegistrationBean<EventRegistrationServlet> registration =
                new ServletRegistrationBean<>(servlet, "/servlets/register");
        registration.setLoadOnStartup(3);
        registration.setName("EventRegistrationServlet");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RequestAuditAndEncodingFilter> requestAuditFilterRegistration(
            RequestAuditAndEncodingFilter filter) {
        // Register the Servlet Filter intercepting web requests
        FilterRegistrationBean<RequestAuditAndEncodingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        // Intercept both servlet paths and MVC routes
        registration.addUrlPatterns("/*");
        // Assign filter execution priority
        registration.setOrder(1);
        registration.setName("RequestAuditAndEncodingFilter");
        return registration;
    }

    @Bean
    public ServletListenerRegistrationBean<AppSessionListener> sessionListenerRegistration(
            AppSessionListener listener) {
        // Register the HttpSessionListener into the embedded servlet container
        ServletListenerRegistrationBean<AppSessionListener> listenerBean = new ServletListenerRegistrationBean<>();
        listenerBean.setListener(listener);
        return listenerBean;
    }
}
