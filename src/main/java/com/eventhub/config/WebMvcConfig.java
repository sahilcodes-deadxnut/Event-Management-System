package com.eventhub.config;

import com.eventhub.security.AdminSecurityInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminSecurityInterceptor adminSecurityInterceptor;

    @Autowired
    public WebMvcConfig(AdminSecurityInterceptor adminSecurityInterceptor) {
        this.adminSecurityInterceptor = adminSecurityInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Enforce admin authentication across all /admin endpoints
        registry.addInterceptor(adminSecurityInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login", "/static/**", "/css/**", "/js/**", "/images/**");
    }
}
