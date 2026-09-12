package com.example.demo.dto;

import java.util.Set;
import java.util.UUID;

public record AdminToolConfigResponse(
        UUID id,
        String tenantSlug,
        String name,
        String callbackUrl,
        String secretRef,
        String signingKeyId,
        Set<String> allowedScopes,
        boolean active) {
}
