package com.example.demo.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
@Table(name = "tenant_tool_configs")
public class TenantToolConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 500)
    private String callbackUrl;

    @Column(nullable = false, length = 500)
    private String signingSecret;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tenant_tool_allowed_scopes", joinColumns = @JoinColumn(name = "tool_config_id"))
    @Column(name = "scope", nullable = false, length = 80)
    private Set<String> allowedScopes = new HashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected TenantToolConfig() {
    }

    public TenantToolConfig(
            Tenant tenant,
            String name,
            String callbackUrl,
            String signingSecret,
            Set<String> allowedScopes,
            boolean active) {
        this.tenant = tenant;
        this.name = name;
        this.callbackUrl = callbackUrl;
        this.signingSecret = signingSecret;
        this.allowedScopes = allowedScopes == null ? new HashSet<>() : new HashSet<>(allowedScopes);
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

    public String getName() {
        return name;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public String getSigningSecret() {
        return signingSecret;
    }

    public Set<String> getAllowedScopes() {
        return allowedScopes;
    }

    public boolean isActive() {
        return active;
    }
}
