package com.example.demo.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_user_auth_configs")
public class TenantUserAuthConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 300)
    private String issuer;

    @Column(nullable = false, length = 160)
    private String audience = "ai-friend-chat";

    @Column(nullable = false, length = 2000)
    private String jwksUri;

    @Column(nullable = false, length = 20)
    private String allowedAlgorithm = "RS256";

    @Column(nullable = false)
    private int maxTokenLifetimeSeconds = 60;

    @Column(nullable = false)
    private int jwksCacheTtlSeconds = 300;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected TenantUserAuthConfig() {
    }

    public TenantUserAuthConfig(
            Tenant tenant,
            String issuer,
            String audience,
            String jwksUri,
            String allowedAlgorithm,
            int maxTokenLifetimeSeconds,
            int jwksCacheTtlSeconds,
            boolean active) {
        this.tenant = tenant;
        this.issuer = issuer;
        this.audience = audience;
        this.jwksUri = jwksUri;
        this.allowedAlgorithm = allowedAlgorithm;
        this.maxTokenLifetimeSeconds = maxTokenLifetimeSeconds;
        this.jwksCacheTtlSeconds = jwksCacheTtlSeconds;
        this.active = active;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getAudience() {
        return audience;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public String getAllowedAlgorithm() {
        return allowedAlgorithm;
    }

    public int getMaxTokenLifetimeSeconds() {
        return maxTokenLifetimeSeconds;
    }

    public int getJwksCacheTtlSeconds() {
        return jwksCacheTtlSeconds;
    }

    public boolean isActive() {
        return active;
    }

    public void update(
            String issuer,
            String audience,
            String jwksUri,
            String allowedAlgorithm,
            int maxTokenLifetimeSeconds,
            int jwksCacheTtlSeconds,
            boolean active) {
        this.issuer = issuer;
        this.audience = audience;
        this.jwksUri = jwksUri;
        this.allowedAlgorithm = allowedAlgorithm;
        this.maxTokenLifetimeSeconds = maxTokenLifetimeSeconds;
        this.jwksCacheTtlSeconds = jwksCacheTtlSeconds;
        this.active = active;
    }
}
