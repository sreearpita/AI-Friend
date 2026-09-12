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
@Table(name = "api_keys")
public class ApiKey {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, unique = true, length = 128)
    private String keyHash;

    @Column(nullable = false, length = 32)
    private String keyPrefix;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(nullable = false)
    private boolean active = true;

    private Instant expiresAt;

    private Instant revokedAt;

    private Instant lastUsedAt;

    @Column(length = 160)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ApiKey() {
    }

    public ApiKey(Tenant tenant, String keyHash, String keyPrefix, String label, Instant expiresAt, String createdBy) {
        this.tenant = tenant;
        this.keyHash = keyHash;
        this.keyPrefix = keyPrefix;
        this.label = label;
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
    }

    public ApiKey(Tenant tenant, String keyHash, String label) {
        this(tenant, keyHash, "legacy", label, null, null);
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

    public String getKeyHash() {
        return keyHash;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public String getLabel() {
        return label;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markUsed(Instant usedAt) {
        this.lastUsedAt = usedAt;
    }

    public void revoke(Instant revokedAt) {
        this.active = false;
        this.revokedAt = revokedAt;
    }
}
