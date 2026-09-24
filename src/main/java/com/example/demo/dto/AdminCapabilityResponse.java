package com.example.demo.dto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AdminCapabilityResponse(
        UUID id,
        String tenantSlug,
        String capabilityKey,
        String displayName,
        String description,
        List<String> triggerPhrases,
        Set<String> requiredScopes,
        List<String> toolNames,
        Set<String> retrievalTopics,
        int priority,
        boolean active) {
}
