package com.example.demo.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminToolConfigRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 500) String callbackUrl,
        @NotBlank @Size(max = 500) String secretRef,
        @NotBlank @Size(max = 80) String signingKeyId,
        @Size(max = 160) String contractVersion,
        Set<@Size(max = 80) String> allowedScopes,
        boolean active) {
    public AdminToolConfigRequest {
        if (contractVersion == null || contractVersion.isBlank()) {
            contractVersion = switch (name) {
                case "cycle-summary" -> "flowelle.cycle-summary.v1";
                case "user-preferences" -> "flowelle.user-preferences.v1";
                default -> "generic.host-tool.v1";
            };
        }
    }
}
