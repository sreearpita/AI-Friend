package com.example.demo.dto;

import java.util.Set;
import java.util.UUID;

public record FlowelleUserPreferencesRequest(
        UUID requestId,
        String externalUserId,
        UUID sessionId,
        Set<String> scopes,
        String locale,
        String contractVersion) {
}
