package com.example.demo.dto;

import java.util.Set;
import java.util.UUID;

public record FlowelleUserPreferencesRequest(
        UUID requestId,
        String externalUserId,
        UUID sessionId,
        Set<String> scopes,
        String locale,
        String contractVersion,
        String authorizationJti,
        boolean aiCoachEnabled) {
    public FlowelleUserPreferencesRequest(
            UUID requestId,
            String externalUserId,
            UUID sessionId,
            Set<String> scopes,
            String locale,
            String contractVersion) {
        this(requestId, externalUserId, sessionId, scopes, locale, contractVersion, null, true);
    }
}
