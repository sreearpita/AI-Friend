package com.example.demo.dto;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record FlowelleContextRequest(UUID requestId, String externalUserId, UUID sessionId, Set<String> scopes,
        String locale, String contractVersion, String authorizationJti, boolean aiCoachEnabled,
        Map<String, Object> parameters) {
}
