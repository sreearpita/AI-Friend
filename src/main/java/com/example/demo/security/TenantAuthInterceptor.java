package com.example.demo.security;

import com.example.demo.config.AiFriendProperties;
import com.example.demo.model.Tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantAuthInterceptor implements HandlerInterceptor {
    public static final String TENANT_ATTRIBUTE = "authenticatedTenant";

    private final AiFriendProperties properties;
    private final TenantAuthService tenantAuthService;

    public TenantAuthInterceptor(AiFriendProperties properties, TenantAuthService tenantAuthService) {
        this.properties = properties;
        this.tenantAuthService = tenantAuthService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String apiKey = request.getHeader(properties.getSecurity().getApiKeyHeader());
        Tenant tenant = tenantAuthService.authenticate(apiKey);
        request.setAttribute(TENANT_ATTRIBUTE, tenant);
        return true;
    }
}
