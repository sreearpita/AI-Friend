package com.example.demo.config;

import com.example.demo.security.TenantAuthInterceptor;
import com.example.demo.security.AdminAuthInterceptor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(AiFriendProperties.class)
public class WebConfig implements WebMvcConfigurer {
    private final AiFriendProperties properties;
    private final TenantAuthInterceptor tenantAuthInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;

    public WebConfig(
            AiFriendProperties properties,
            TenantAuthInterceptor tenantAuthInterceptor,
            AdminAuthInterceptor adminAuthInterceptor) {
        this.properties = properties;
        this.tenantAuthInterceptor = tenantAuthInterceptor;
        this.adminAuthInterceptor = adminAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantAuthInterceptor)
                .addPathPatterns("/v1/**", "/v2/**");
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/internal/admin/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(properties.getSecurity().getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }
}
