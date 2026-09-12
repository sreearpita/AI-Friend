package com.example.demo.dto;

import java.util.UUID;

public record AdminTenantResponse(
        UUID id,
        String slug,
        String displayName,
        boolean active) {
}
