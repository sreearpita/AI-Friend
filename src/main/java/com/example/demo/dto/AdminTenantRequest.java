package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminTenantRequest(
        @NotBlank @Size(max = 80) String slug,
        @NotBlank @Size(max = 160) String displayName,
        boolean active) {
}
