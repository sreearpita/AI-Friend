package com.example.demo.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminApiKeyCreateResponse(
        UUID id,
        String key,
        String keyPrefix,
        String label,
        Instant expiresAt) {
}
