package com.example.demo.security;

import java.time.Duration;
import java.time.Instant;

import com.example.demo.config.AiFriendProperties;
import com.example.demo.exception.ApiException;
import com.example.demo.model.AuthenticatedUserContext;
import com.example.demo.model.Tenant;
import com.example.demo.service.PlatformMetrics;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RequestGuardService {
    private final AiFriendProperties properties;
    private final DistributedStateStore store;
    private final PlatformMetrics platformMetrics;

    public RequestGuardService(AiFriendProperties properties, DistributedStateStore store, PlatformMetrics platformMetrics) {
        this.properties = properties;
        this.store = store;
        this.platformMetrics = platformMetrics;
    }

    public void accept(Tenant tenant, AuthenticatedUserContext userContext) {
        Duration replayTtl = Duration.between(Instant.now(), userContext.expiresAt());
        if (replayTtl.isNegative() || replayTtl.isZero()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_USER_CONTEXT", "JWT is expired.");
        }
        String replayKey = "jti:" + tenant.getSlug() + ":" + userContext.jwtId();
        if (!store.putIfAbsent(replayKey, "1", replayTtl)) {
            platformMetrics.recordAuthFailure("replayed_user_context");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REPLAYED_USER_CONTEXT", "JWT has already been used.");
        }
        if (!properties.getRateLimits().isEnabled()) {
            return;
        }
        check("user:" + tenant.getSlug() + ":" + userContext.externalUserId() + ":minute",
                Duration.ofMinutes(1),
                properties.getRateLimits().getUserPerMinute(),
                "RATE_LIMIT_EXCEEDED",
                "User rate limit exceeded.");
        check("tenant:" + tenant.getSlug() + ":minute",
                Duration.ofMinutes(1),
                properties.getRateLimits().getTenantPerMinute(),
                "RATE_LIMIT_EXCEEDED",
                "Tenant rate limit exceeded.");
        check("tenant:" + tenant.getSlug() + ":day",
                Duration.ofDays(1),
                properties.getRateLimits().getTenantPerDay(),
                "QUOTA_EXCEEDED",
                "Tenant daily quota exceeded.");
    }

    private void check(String key, Duration window, int limit, String code, String message) {
        LimitResult result = store.incrementAndCheck(key, window, limit);
        if (!result.allowed()) {
            platformMetrics.recordRateLimit(key.split(":")[1], code);
            throw new RateLimitException(code, message, result.retryAfterSeconds());
        }
    }
}
