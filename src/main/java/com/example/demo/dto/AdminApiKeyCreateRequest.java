package com.example.demo.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminApiKeyCreateRequest(
        @NotBlank @Size(max = 120) String label,
        Instant expiresAt) {
}
