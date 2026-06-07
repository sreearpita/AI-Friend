package com.example.demo.security;

import com.example.demo.config.AiFriendProperties;
import com.example.demo.model.ApiKey;
import com.example.demo.model.Tenant;
import com.example.demo.repository.ApiKeyRepository;
import com.example.demo.repository.TenantRepository;

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
    private final ApiKeyHasher apiKeyHasher;

    public DemoTenantSeeder(
            AiFriendProperties properties,
            TenantRepository tenantRepository,
            ApiKeyRepository apiKeyRepository,
            ApiKeyHasher apiKeyHasher) {
        this.properties = properties;
        this.tenantRepository = tenantRepository;
        this.apiKeyRepository = apiKeyRepository;
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
    }
}
