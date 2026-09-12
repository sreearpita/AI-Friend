package com.example.demo.model;

import java.time.Instant;
import java.util.Set;

public record AuthenticatedUserContext(
        String externalUserId,
        String issuer,
        String jwtId,
        Instant issuedAt,
        Instant expiresAt,
        Set<String> scopes) {
}
