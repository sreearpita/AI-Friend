package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserAuthConfigRequest(
        @NotBlank @Size(max = 300) String issuer,
        @NotBlank @Size(max = 160) String audience,
        @NotBlank @Size(max = 500) String jwksUri,
        @NotBlank @Size(max = 20) String allowedAlgorithm,
        @Min(1) @Max(300) int maxTokenLifetimeSeconds,
        @Min(1) @Max(3600) int jwksCacheTtlSeconds,
        boolean active) {
}
