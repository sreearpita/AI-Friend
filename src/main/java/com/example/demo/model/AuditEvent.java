package com.example.demo.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(length = 128)
    private String externalUserId;

    private UUID sessionId;

    @Column(nullable = false, length = 120)
    private String eventType;

    @Lob
    @Column(nullable = false)
    private String metadataJson;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditEvent() {
    }

    public AuditEvent(UUID tenantId, String externalUserId, UUID sessionId, String eventType, String metadataJson) {
        this.tenantId = tenantId;
        this.externalUserId = externalUserId;
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.metadataJson = metadataJson;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getEventType() {
        return eventType;
    }

    public String getMetadataJson() {
        return metadataJson;
    }
}
