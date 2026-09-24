package com.example.demo.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_capabilities")
public class TenantCapability {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 100)
    private String capabilityKey;

    @Column(nullable = false, length = 160)
    private String displayName;

    @Column(nullable = false, length = 500)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tenant_capability_triggers", joinColumns = @JoinColumn(name = "capability_id"))
    @Column(name = "trigger_phrase", nullable = false, length = 120)
    private List<String> triggerPhrases = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tenant_capability_scopes", joinColumns = @JoinColumn(name = "capability_id"))
    @Column(name = "scope", nullable = false, length = 80)
    private Set<String> requiredScopes = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tenant_capability_tools", joinColumns = @JoinColumn(name = "capability_id"))
    @OrderColumn(name = "tool_order")
    @Column(name = "tool_name", nullable = false, length = 80)
    private List<String> toolNames = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tenant_capability_topics", joinColumns = @JoinColumn(name = "capability_id"))
    @Column(name = "topic", nullable = false, length = 100)
    private Set<String> retrievalTopics = new HashSet<>();

    @Column(nullable = false)
    private int priority = 100;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected TenantCapability() {
    }

    public TenantCapability(Tenant tenant, String capabilityKey, String displayName, String description,
            List<String> triggerPhrases, Set<String> requiredScopes, List<String> toolNames,
            Set<String> retrievalTopics, int priority, boolean active) {
        this.tenant = tenant;
        this.capabilityKey = capabilityKey;
        this.displayName = displayName;
        this.description = description;
        this.triggerPhrases = triggerPhrases == null ? new ArrayList<>() : new ArrayList<>(triggerPhrases);
        this.requiredScopes = requiredScopes == null ? new HashSet<>() : new HashSet<>(requiredScopes);
        this.toolNames = toolNames == null ? new ArrayList<>() : new ArrayList<>(toolNames);
        this.retrievalTopics = retrievalTopics == null ? new HashSet<>() : new HashSet<>(retrievalTopics);
        this.priority = priority;
        this.active = active;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public String getCapabilityKey() { return capabilityKey; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public List<String> getTriggerPhrases() { return triggerPhrases; }
    public Set<String> getRequiredScopes() { return requiredScopes; }
    public List<String> getToolNames() { return toolNames; }
    public Set<String> getRetrievalTopics() { return retrievalTopics; }
    public int getPriority() { return priority; }
    public boolean isActive() { return active; }

    public void update(String displayName, String description, List<String> triggerPhrases,
            Set<String> requiredScopes, List<String> toolNames, Set<String> retrievalTopics,
            int priority, boolean active) {
        this.displayName = displayName;
        this.description = description;
        this.triggerPhrases = triggerPhrases == null ? new ArrayList<>() : new ArrayList<>(triggerPhrases);
        this.requiredScopes = requiredScopes == null ? new HashSet<>() : new HashSet<>(requiredScopes);
        this.toolNames = toolNames == null ? new ArrayList<>() : new ArrayList<>(toolNames);
        this.retrievalTopics = retrievalTopics == null ? new HashSet<>() : new HashSet<>(retrievalTopics);
        this.priority = priority;
        this.active = active;
    }
}
