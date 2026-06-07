package com.example.demo.security;

import java.util.Optional;

import com.example.demo.config.AiFriendProperties;
import com.example.demo.exception.ApiException;
import com.example.demo.model.ApiKey;
import com.example.demo.model.Tenant;
import com.example.demo.repository.ApiKeyRepository;
import com.example.demo.repository.TenantRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TenantAuthService {
    private final ApiKeyRepository apiKeyRepository;
    private final TenantRepository tenantRepository;
    private final ApiKeyHasher apiKeyHasher;
    private final AiFriendProperties properties;

    public TenantAuthService(
            ApiKeyRepository apiKeyRepository,
            TenantRepository tenantRepository,
            ApiKeyHasher apiKeyHasher,
            AiFriendProperties properties) {
        this.apiKeyRepository = apiKeyRepository;
        this.tenantRepository = tenantRepository;
        this.apiKeyHasher = apiKeyHasher;
        this.properties = properties;
    }

    public Tenant authenticate(String rawApiKey) {
        if (!StringUtils.hasText(rawApiKey)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_MISSING_API_KEY", "Missing tenant API key.");
        }

        String keyHash = apiKeyHasher.hash(rawApiKey);
        ApiKey apiKey = apiKeyRepository.findActiveByKeyHashWithTenant(keyHash)
                .filter(candidate -> apiKeyHasher.matches(rawApiKey, candidate.getKeyHash()))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "AUTH_INVALID_API_KEY",
                        "Invalid tenant API key."));

        Tenant tenant = apiKey.getTenant();
        if (!tenant.isActive()) {
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
