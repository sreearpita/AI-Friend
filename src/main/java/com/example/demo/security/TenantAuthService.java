package com.example.demo.security;

import java.util.Optional;
import java.time.Instant;

import com.example.demo.config.AiFriendProperties;
import com.example.demo.exception.ApiException;
import com.example.demo.model.ApiKey;
import com.example.demo.model.Tenant;
import com.example.demo.repository.ApiKeyRepository;
import com.example.demo.repository.TenantRepository;
import com.example.demo.service.PlatformMetrics;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TenantAuthService {
    private final ApiKeyRepository apiKeyRepository;
    private final TenantRepository tenantRepository;
    private final ApiKeyHasher apiKeyHasher;
    private final AiFriendProperties properties;
    private final PlatformMetrics platformMetrics;

    public TenantAuthService(
            ApiKeyRepository apiKeyRepository,
            TenantRepository tenantRepository,
            ApiKeyHasher apiKeyHasher,
            AiFriendProperties properties,
            PlatformMetrics platformMetrics) {
        this.apiKeyRepository = apiKeyRepository;
        this.tenantRepository = tenantRepository;
        this.apiKeyHasher = apiKeyHasher;
        this.properties = properties;
        this.platformMetrics = platformMetrics;
    }

    @Transactional
    public Tenant authenticate(String rawApiKey) {
        if (!StringUtils.hasText(rawApiKey)) {
            platformMetrics.recordAuthFailure("missing_api_key");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_MISSING_API_KEY", "Missing tenant API key.");
        }

        String keyHash = apiKeyHasher.hash(rawApiKey);
        ApiKey apiKey = apiKeyRepository.findUsableByKeyHashWithTenant(keyHash, Instant.now())
                .filter(candidate -> apiKeyHasher.matches(rawApiKey, candidate.getKeyHash()))
                .orElseThrow(() -> {
                    platformMetrics.recordAuthFailure("invalid_api_key");
                    return new ApiException(
                            HttpStatus.UNAUTHORIZED,
                            "AUTH_INVALID_API_KEY",
                            "Invalid tenant API key.");
                });
        apiKey.markUsed(Instant.now());

        Tenant tenant = apiKey.getTenant();
        if (!tenant.isActive()) {
            platformMetrics.recordAuthFailure("inactive_tenant");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INACTIVE_TENANT", "Tenant is inactive.");
        }
        return tenant;
    }

    public Tenant requireDemoTenant() {
        return tenantRepository.findBySlug(properties.getSecurity().getDemoTenantSlug())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "DEMO_TENANT_UNAVAILABLE",
                        "Demo tenant is not configured."));
    }

    public Optional<Tenant> findDemoTenant() {
        return tenantRepository.findBySlug(properties.getSecurity().getDemoTenantSlug());
    }
}
