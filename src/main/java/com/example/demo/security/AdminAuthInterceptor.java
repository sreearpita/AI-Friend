package com.example.demo.security;

import com.example.demo.config.AiFriendProperties;
import com.example.demo.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {
    public static final String ADMIN_ACTOR_ATTRIBUTE = "authenticatedAdminActor";

    private final AiFriendProperties properties;
    private final JwtVerifier jwtVerifier;

    public AdminAuthInterceptor(AiFriendProperties properties, JwtVerifier jwtVerifier) {
        this.properties = properties;
        this.jwtVerifier = jwtVerifier;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        AiFriendProperties.Admin admin = properties.getAdmin();
        if (!StringUtils.hasText(admin.getIssuer()) || !StringUtils.hasText(admin.getJwksUri())) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ADMIN_AUTH_NOT_CONFIGURED", "Admin OIDC is not configured.");
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "ADMIN_AUTH_MISSING_TOKEN", "Missing admin bearer token.");
        }
        ParsedJwt jwt = jwtVerifier.verifyAdmin(
                header.substring("Bearer ".length()),
                admin.getIssuer(),
                admin.getAudience(),
                admin.getJwksUri(),
                admin.getMaxTokenLifetimeSeconds());
        if (!hasRole(jwt.claims(), admin.getRequiredRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_AUTH_INSUFFICIENT_ROLE", "Admin role is required.");
        }
        request.setAttribute(ADMIN_ACTOR_ATTRIBUTE, jwt.claims().path("sub").asText());
        return true;
    }

    private boolean hasRole(JsonNode claims, String requiredRole) {
        if (contains(claims.path("roles"), requiredRole) || contains(claims.path("role"), requiredRole)) {
            return true;
        }
        JsonNode realmRoles = claims.path("realm_access").path("roles");
        return contains(realmRoles, requiredRole);
    }

    private boolean contains(JsonNode node, String expected) {
        if (node.isTextual()) {
            return expected.equals(node.asText());
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (expected.equals(item.asText())) {
                    return true;
                }
            }
        }
        return false;
    }
}
