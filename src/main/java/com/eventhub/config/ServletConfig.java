package com.eventhub.config;

import com.eventhub.filter.RateLimitingSecurityFilter;
import com.eventhub.filter.RequestAuditAndEncodingFilter;
import com.eventhub.listener.AppSessionListener;
import com.eventhub.servlet.*;
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
        // Register CSV export HttpServlet mapped to '/export/participants.csv'
        ServletRegistrationBean<ParticipantCsvExportServlet> reg =
                new ServletRegistrationBean<>(servlet, "/export/participants.csv");
        reg.setLoadOnStartup(1);
        reg.setName("ParticipantCsvExportServlet");
        return reg;
    }

    @Bean
    public ServletRegistrationBean<EventListServlet> eventListServletRegistration(
            EventListServlet servlet) {
        // Register EventListServlet mapped to '/servlets/events'
        ServletRegistrationBean<EventListServlet> reg =
                new ServletRegistrationBean<>(servlet, "/servlets/events");
        reg.setLoadOnStartup(2);
        reg.setName("EventListServlet");
        return reg;
    }

    @Bean
    public ServletRegistrationBean<EventRegistrationServlet> eventRegistrationServletRegistration(
            EventRegistrationServlet servlet) {
        // Register EventRegistrationServlet mapped to '/servlets/register'
        ServletRegistrationBean<EventRegistrationServlet> reg =
                new ServletRegistrationBean<>(servlet, "/servlets/register");
        reg.setLoadOnStartup(3);
        reg.setName("EventRegistrationServlet");
        return reg;
    }

    @Bean
    public ServletRegistrationBean<TicketBadgeQrServlet> ticketBadgeQrServletRegistration(
            TicketBadgeQrServlet servlet) {
        // Register TicketBadgeQrServlet mapped to SVG badge endpoints
        ServletRegistrationBean<TicketBadgeQrServlet> reg =
                new ServletRegistrationBean<>(servlet, "/servlets/ticket-badge", "/export/ticket-badge.svg");
        reg.setLoadOnStartup(4);
        reg.setName("TicketBadgeQrServlet");
        return reg;
    }

    @Bean
    public ServletRegistrationBean<EventJsonApiServlet> eventJsonApiServletRegistration(
            EventJsonApiServlet servlet) {
        // Register EventJsonApiServlet mapped to RESTful JSON API path '/api/events/*'
        ServletRegistrationBean<EventJsonApiServlet> reg =
                new ServletRegistrationBean<>(servlet, "/api/events/*");
        reg.setLoadOnStartup(5);
        reg.setName("EventJsonApiServlet");
        return reg;
    }

    @Bean
    public ServletRegistrationBean<LiveCapacitySseServlet> liveCapacitySseServletRegistration(
            LiveCapacitySseServlet servlet) {
        // Register Asynchronous SSE Servlet mapped to '/servlets/stream/capacity'
        ServletRegistrationBean<LiveCapacitySseServlet> reg =
                new ServletRegistrationBean<>(servlet, "/servlets/stream/capacity");
        reg.setLoadOnStartup(6);
        reg.setAsyncSupported(true);
        reg.setName("LiveCapacitySseServlet");
        return reg;
    }

    @Bean
    public FilterRegistrationBean<RequestAuditAndEncodingFilter> requestAuditFilterRegistration(
            RequestAuditAndEncodingFilter filter) {
        // Register RequestAuditAndEncodingFilter executing first (Order 1)
        FilterRegistrationBean<RequestAuditAndEncodingFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(filter);
        reg.addUrlPatterns("/*");
        reg.setOrder(1);
        reg.setName("RequestAuditAndEncodingFilter");
        return reg;
    }

    @Bean
    public FilterRegistrationBean<RateLimitingSecurityFilter> rateLimitingFilterRegistration(
            RateLimitingSecurityFilter filter) {
        // Register RateLimitingSecurityFilter executing second (Order 2)
        FilterRegistrationBean<RateLimitingSecurityFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(filter);
        reg.addUrlPatterns("/*");
        reg.setOrder(2);
        reg.setName("RateLimitingSecurityFilter");
        return reg;
    }

    @Bean
    public ServletListenerRegistrationBean<AppSessionListener> sessionListenerRegistration(
            AppSessionListener listener) {
        // Register session tracking listener
        ServletListenerRegistrationBean<AppSessionListener> listenerBean = new ServletListenerRegistrationBean<>();
        listenerBean.setListener(listener);
        return listenerBean;
    }
}
