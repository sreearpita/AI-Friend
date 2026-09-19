package com.example.demo.security;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import com.example.demo.exception.ApiException;
import com.example.demo.model.AuthenticatedUserContext;
import com.example.demo.model.Tenant;
import com.example.demo.model.TenantUserAuthConfig;
import com.example.demo.repository.TenantUserAuthConfigRepository;
import com.example.demo.service.PlatformMetrics;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserContextAuthService {
    private final TenantUserAuthConfigRepository authConfigRepository;
    private final JwtVerifier jwtVerifier;
    private final RequestGuardService requestGuardService;
    private final PlatformMetrics platformMetrics;

    public UserContextAuthService(
            TenantUserAuthConfigRepository authConfigRepository,
            JwtVerifier jwtVerifier,
            RequestGuardService requestGuardService,
            PlatformMetrics platformMetrics) {
        this.authConfigRepository = authConfigRepository;
        this.jwtVerifier = jwtVerifier;
        this.requestGuardService = requestGuardService;
        this.platformMetrics = platformMetrics;
    }

    public AuthenticatedUserContext authenticate(Tenant tenant, String userContextJwt) {
        if (!StringUtils.hasText(userContextJwt)) {
            platformMetrics.recordAuthFailure("missing_user_context");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_MISSING_USER_CONTEXT", "Missing user context JWT.");
        }
        TenantUserAuthConfig config = authConfigRepository.findByTenantIdAndActiveTrue(tenant.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "USER_CONTEXT_AUTH_NOT_CONFIGURED",
                        "Tenant user-context authentication is not configured."));
        ParsedJwt jwt;
        try {
            jwt = jwtVerifier.verify(userContextJwt, config);
        } catch (ApiException exception) {
            platformMetrics.recordAuthFailure("invalid_user_context");
            throw exception;
        }
        JsonNode claims = jwt.claims();
        if (!tenant.getSlug().equals(claims.path("tenant").asText())) {
            platformMetrics.recordAuthFailure("invalid_user_context_tenant");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_USER_CONTEXT", "JWT tenant is invalid.");
        }

        Set<String> scopes = scopes(claims.path("scope"));
        if (scopes.isEmpty() || !scopes.contains("wellness:chat")) {
            platformMetrics.recordAuthFailure("insufficient_user_scope");
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_INSUFFICIENT_SCOPE", "User context lacks wellness chat scope.");
        }

        AuthenticatedUserContext userContext = new AuthenticatedUserContext(
                claims.path("sub").asText(),
                claims.path("iss").asText(),
                claims.path("jti").asText(),
                Instant.ofEpochSecond(claims.path("iat").asLong()),
                Instant.ofEpochSecond(claims.path("exp").asLong()),
                scopes,
                claims.path("aiCoachEnabled").asBoolean(false));
        requestGuardService.accept(tenant, userContext);
        return userContext;
    }

    private Set<String> scopes(JsonNode scopeClaim) {
        Set<String> scopes = new LinkedHashSet<>();
        if (scopeClaim.isTextual()) {
            for (String scope : scopeClaim.asText().split(" ")) {
                addScope(scopes, scope);
            }
        } else if (scopeClaim.isArray()) {
            for (JsonNode item : scopeClaim) {
                addScope(scopes, item.asText());
            }
        }
        return Set.copyOf(scopes);
    }

    private void addScope(Set<String> scopes, String scope) {
        if (StringUtils.hasText(scope)) {
            scopes.add(scope.toLowerCase(Locale.ROOT));
        }
    }
}
