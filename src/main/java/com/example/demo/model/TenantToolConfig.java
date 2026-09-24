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

    @Column(length = 500)
    private String secretRef;

    @Column(nullable = false, length = 80)
    private String signingKeyId = "dev-v1";

    @Column(nullable = false, length = 160)
    private String contractVersion = "generic.host-tool.v1";

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
            String secretRef,
            String signingKeyId,
            Set<String> allowedScopes,
            boolean active) {
        this(tenant, name, callbackUrl, secretRef, signingKeyId, "generic.host-tool.v1", allowedScopes, active);
    }

    public TenantToolConfig(
            Tenant tenant,
            String name,
            String callbackUrl,
            String secretRef,
            String signingKeyId,
            String contractVersion,
            Set<String> allowedScopes,
            boolean active) {
        this.tenant = tenant;
        this.name = name;
        this.callbackUrl = callbackUrl;
        this.signingSecret = "";
        this.secretRef = secretRef;
        this.signingKeyId = signingKeyId;
        this.contractVersion = contractVersion == null || contractVersion.isBlank()
                ? "generic.host-tool.v1" : contractVersion;
        this.allowedScopes = allowedScopes == null ? new HashSet<>() : new HashSet<>(allowedScopes);
        this.active = active;
    }

    public TenantToolConfig(
            Tenant tenant,
            String name,
            String callbackUrl,
            String secretRef,
            Set<String> allowedScopes,
            boolean active) {
        this(tenant, name, callbackUrl, secretRef, "dev-v1", "generic.host-tool.v1", allowedScopes, active);
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

    public String getSecretRef() {
        return secretRef;
    }

    public String getSigningKeyId() {
        return signingKeyId;
    }

    public String getContractVersion() {
        return contractVersion;
    }

    public Set<String> getAllowedScopes() {
        return allowedScopes;
    }

    public boolean isActive() {
        return active;
    }

    public void update(
            String callbackUrl,
            String secretRef,
            String signingKeyId,
            String contractVersion,
            Set<String> allowedScopes,
            boolean active) {
        this.callbackUrl = callbackUrl;
        this.secretRef = secretRef;
        this.signingKeyId = signingKeyId;
        this.contractVersion = contractVersion == null || contractVersion.isBlank()
                ? "generic.host-tool.v1" : contractVersion;
        this.allowedScopes = allowedScopes == null ? new HashSet<>() : new HashSet<>(allowedScopes);
        this.active = active;
    }

    public void update(
            String callbackUrl,
            String secretRef,
            String signingKeyId,
            Set<String> allowedScopes,
            boolean active) {
        update(callbackUrl, secretRef, signingKeyId, this.contractVersion, allowedScopes, active);
    }
}
