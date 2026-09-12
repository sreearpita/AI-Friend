package com.example.demo.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminApiKeyMetadataResponse(
        UUID id,
        String keyPrefix,
        String label,
        boolean active,
        Instant expiresAt,
        Instant revokedAt,
        Instant lastUsedAt,
        Instant createdAt,
        String createdBy) {
}
