package com.example.demo.service;

import java.time.Instant;
import java.util.List;

import com.example.demo.dto.AdminApiKeyCreateRequest;
import com.example.demo.dto.AdminApiKeyCreateResponse;
import com.example.demo.dto.AdminApiKeyMetadataResponse;
import com.example.demo.dto.AdminTenantRequest;
import com.example.demo.dto.AdminTenantResponse;
import com.example.demo.dto.AdminToolConfigRequest;
import com.example.demo.dto.AdminToolConfigResponse;
import com.example.demo.dto.AdminUserAuthConfigRequest;
import com.example.demo.exception.ApiException;
import com.example.demo.model.ApiKey;
import com.example.demo.model.Tenant;
import com.example.demo.model.TenantToolConfig;
import com.example.demo.model.TenantUserAuthConfig;
import com.example.demo.repository.ApiKeyRepository;
import com.example.demo.repository.TenantRepository;
import com.example.demo.repository.TenantToolConfigRepository;
import com.example.demo.repository.TenantUserAuthConfigRepository;
import com.example.demo.security.ApiKeyGenerator;
import com.example.demo.security.ApiKeyHasher;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminManagementService {
    private final TenantRepository tenantRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final TenantUserAuthConfigRepository authConfigRepository;
    private final TenantToolConfigRepository toolConfigRepository;
    private final ApiKeyHasher apiKeyHasher;
    private final ApiKeyGenerator apiKeyGenerator;
    private final AuditService auditService;

    public AdminManagementService(
            TenantRepository tenantRepository,
            ApiKeyRepository apiKeyRepository,
            TenantUserAuthConfigRepository authConfigRepository,
            TenantToolConfigRepository toolConfigRepository,
            ApiKeyHasher apiKeyHasher,
            ApiKeyGenerator apiKeyGenerator,
            AuditService auditService) {
        this.tenantRepository = tenantRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.authConfigRepository = authConfigRepository;
        this.toolConfigRepository = toolConfigRepository;
        this.apiKeyHasher = apiKeyHasher;
        this.apiKeyGenerator = apiKeyGenerator;
        this.auditService = auditService;
    }

    @Transactional
    public AdminTenantResponse upsertTenant(AdminTenantRequest request, String actor) {
        Tenant tenant = tenantRepository.findBySlug(request.slug())
                .map(existing -> {
                    existing.update(request.displayName(), request.active());
                    return existing;
                })
                .orElseGet(() -> tenantRepository.save(new Tenant(request.slug(), request.displayName())));
        if (!request.active()) {
            tenant.update(request.displayName(), false);
        }
        auditService.record(tenant, actor, null, "admin.tenant.upsert", java.util.Map.of("tenantSlug", tenant.getSlug()));
        return new AdminTenantResponse(tenant.getId(), tenant.getSlug(), tenant.getDisplayName(), tenant.isActive());
    }

    @Transactional
    public AdminUserAuthConfigRequest configureUserAuth(String tenantSlug, AdminUserAuthConfigRequest request, String actor) {
        Tenant tenant = requireTenant(tenantSlug);
        if (!"RS256".equals(request.allowedAlgorithm())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AUTH_CONFIG", "Only RS256 is supported.");
        }
        TenantUserAuthConfig config = authConfigRepository.findByTenantIdAndActiveTrue(tenant.getId())
                .orElseGet(() -> authConfigRepository.save(new TenantUserAuthConfig(
                        tenant,
                        request.issuer(),
                        request.audience(),
                        request.jwksUri(),
                        request.allowedAlgorithm(),
                        request.maxTokenLifetimeSeconds(),
                        request.jwksCacheTtlSeconds(),
                        request.active())));
        config.update(
                request.issuer(),
                request.audience(),
                request.jwksUri(),
                request.allowedAlgorithm(),
                request.maxTokenLifetimeSeconds(),
                request.jwksCacheTtlSeconds(),
                request.active());
        auditService.record(tenant, actor, null, "admin.user-auth-config.upsert", java.util.Map.of("tenantSlug", tenantSlug));
        return request;
    }

    @Transactional
    public AdminToolConfigResponse configureTool(String tenantSlug, AdminToolConfigRequest request, String actor) {
        Tenant tenant = requireTenant(tenantSlug);
        if (!StringUtils.hasText(request.secretRef()) || !request.secretRef().startsWith("env://")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SECRET_REF", "Tool secretRef must use env://.");
        }
        TenantToolConfig config = toolConfigRepository.findByTenantIdAndName(tenant.getId(), request.name())
                .orElseGet(() -> toolConfigRepository.save(new TenantToolConfig(
                        tenant,
                        request.name(),
                        request.callbackUrl(),
                        request.secretRef(),
                        request.signingKeyId(),
                        request.allowedScopes(),
                        request.active())));
        config.update(
                request.callbackUrl(),
                request.secretRef(),
                request.signingKeyId(),
                request.allowedScopes(),
                request.active());
        auditService.record(tenant, actor, null, "admin.tool-config.upsert", java.util.Map.of("tenantSlug", tenantSlug, "tool", request.name()));
        return toolResponse(tenant, config);
    }

    @Transactional
    public AdminApiKeyCreateResponse createApiKey(String tenantSlug, AdminApiKeyCreateRequest request, String actor) {
        Tenant tenant = requireTenant(tenantSlug);
        String rawKey = apiKeyGenerator.generate();
        String prefix = apiKeyGenerator.prefix(rawKey);
        ApiKey apiKey = apiKeyRepository.save(new ApiKey(
                tenant,
                apiKeyHasher.hash(rawKey),
                prefix,
                request.label(),
                request.expiresAt(),
                actor));
        auditService.record(tenant, actor, null, "admin.api-key.create", java.util.Map.of("tenantSlug", tenantSlug, "keyPrefix", prefix));
        return new AdminApiKeyCreateResponse(apiKey.getId(), rawKey, prefix, apiKey.getLabel(), apiKey.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public List<AdminApiKeyMetadataResponse> listApiKeys(String tenantSlug) {
        Tenant tenant = requireTenant(tenantSlug);
        return apiKeyRepository.findByTenantIdOrderByCreatedAtDesc(tenant.getId()).stream()
                .map(key -> new AdminApiKeyMetadataResponse(
                        key.getId(),
                        key.getKeyPrefix(),
                        key.getLabel(),
                        key.isActive(),
                        key.getExpiresAt(),
                        key.getRevokedAt(),
                        key.getLastUsedAt(),
                        key.getCreatedAt(),
                        key.getCreatedBy()))
                .toList();
    }

    @Transactional
    public void revokeApiKey(String tenantSlug, java.util.UUID keyId, String actor) {
        Tenant tenant = requireTenant(tenantSlug);
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .filter(candidate -> candidate.getTenant().getId().equals(tenant.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "API_KEY_NOT_FOUND", "API key was not found."));
        apiKey.revoke(Instant.now());
        auditService.record(tenant, actor, null, "admin.api-key.revoke", java.util.Map.of("tenantSlug", tenantSlug, "keyPrefix", apiKey.getKeyPrefix()));
    }

    private Tenant requireTenant(String tenantSlug) {
        return tenantRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "Tenant was not found."));
    }

    private AdminToolConfigResponse toolResponse(Tenant tenant, TenantToolConfig config) {
        return new AdminToolConfigResponse(
                config.getId(),
                tenant.getSlug(),
                config.getName(),
                config.getCallbackUrl(),
                config.getSecretRef(),
                config.getSigningKeyId(),
                config.getAllowedScopes(),
                config.isActive());
    }
}
