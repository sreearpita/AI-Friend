package com.example.demo.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminToolConfigRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 500) String callbackUrl,
        @NotBlank @Size(max = 500) String secretRef,
        @NotBlank @Size(max = 80) String signingKeyId,
        Set<@Size(max = 80) String> allowedScopes,
        boolean active) {
}
