package com.example.demo.security;

import java.util.Set;

import com.example.demo.config.AiFriendProperties;
import com.example.demo.model.ApiKey;
import com.example.demo.model.Tenant;
import com.example.demo.model.TenantToolConfig;
import com.example.demo.repository.ApiKeyRepository;
import com.example.demo.repository.TenantRepository;
import com.example.demo.repository.TenantToolConfigRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoTenantSeeder implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(DemoTenantSeeder.class);

    private final AiFriendProperties properties;
    private final TenantRepository tenantRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final TenantToolConfigRepository tenantToolConfigRepository;
    private final ApiKeyHasher apiKeyHasher;

    public DemoTenantSeeder(
            AiFriendProperties properties,
            TenantRepository tenantRepository,
            ApiKeyRepository apiKeyRepository,
            TenantToolConfigRepository tenantToolConfigRepository,
            ApiKeyHasher apiKeyHasher) {
        this.properties = properties;
        this.tenantRepository = tenantRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.tenantToolConfigRepository = tenantToolConfigRepository;
        this.apiKeyHasher = apiKeyHasher;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.getSecurity().isSeedDemoTenant()) {
            return;
        }

        Tenant tenant = tenantRepository.findBySlug(properties.getSecurity().getDemoTenantSlug())
                .orElseGet(() -> tenantRepository.save(new Tenant(
                        properties.getSecurity().getDemoTenantSlug(),
                        properties.getSecurity().getDemoTenantName())));

        String keyHash = apiKeyHasher.hash(properties.getSecurity().getDemoApiKey());
        if (!apiKeyRepository.existsByKeyHash(keyHash)) {
            apiKeyRepository.save(new ApiKey(tenant, keyHash, "Local demo key"));
            logger.info("Seeded demo tenant API key. tenant={}", tenant.getSlug());
        }

        if (properties.getTools().isSeedDemoTools()) {
            seedDemoTool(tenant, "cycle-summary", Set.of("cycle:read", "wellness"));
            seedDemoTool(tenant, "user-preferences", Set.of("preferences:read", "wellness"));
        }
    }

    private void seedDemoTool(Tenant tenant, String name, Set<String> allowedScopes) {
        if (tenantToolConfigRepository.existsByTenantIdAndName(tenant.getId(), name)) {
            return;
        }

        String callbackUrl = properties.getTools().getDemoCallbackUrl() + "/" + name;
        tenantToolConfigRepository.save(new TenantToolConfig(
                tenant,
                name,
                callbackUrl,
                properties.getTools().getDemoSigningSecret(),
                properties.getTools().getDemoSigningKeyId(),
                allowedScopes,
                true));
        logger.info("Seeded demo tenant tool config. tenant={} tool={}", tenant.getSlug(), name);
    }
}
