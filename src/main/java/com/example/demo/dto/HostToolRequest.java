package com.example.demo.dto;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record HostToolRequest(
        UUID requestId,
        String tenantSlug,
        String externalUserId,
        UUID sessionId,
        String toolName,
        Set<String> scopes,
        String locale,
        Map<String, Object> parameters) {
}
