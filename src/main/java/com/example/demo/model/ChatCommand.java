package com.example.demo.model;

import java.util.Set;
import java.util.UUID;

import com.example.demo.dto.ChatMessageRequest;
import com.example.demo.dto.ChatV2MessageRequest;

public record ChatCommand(
        UUID requestId,
        String externalUserId,
        UUID sessionId,
        String message,
        String locale,
        Set<String> scopes,
        String authorizationJti) {
    public static ChatCommand fromV1(ChatMessageRequest request) {
        return new ChatCommand(
                UUID.randomUUID(),
                request.externalUserId(),
                request.sessionId(),
                request.message(),
                request.locale(),
                request.scopes(),
                null);
    }

    public static ChatCommand fromV2(ChatV2MessageRequest request, AuthenticatedUserContext userContext) {
        return new ChatCommand(
                UUID.randomUUID(),
                userContext.externalUserId(),
                request.sessionId(),
                request.message(),
                request.locale(),
                userContext.scopes(),
                userContext.jwtId());
    }
}
