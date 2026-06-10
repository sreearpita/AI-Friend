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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "content_chunks")
public class ContentChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private ContentSource source;

    @Column(nullable = false, length = 120)
    private String topic;

    @Lob
    @Column(nullable = false)
    private String chunkText;

    @Column(nullable = false, length = 1000)
    private String keywords;

    @Column(nullable = false, length = 40)
    private String reviewStatus = "DRAFT";

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ContentChunk() {
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public ContentSource getSource() {
        return source;
    }

    public String getTopic() {
        return topic;
    }

    public String getChunkText() {
        return chunkText;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public boolean isActive() {
        return active;
    }
}
