package com.example.demo.dto;

import java.util.UUID;

public class ConversationResponse {
    private final UUID conversationId;

    public ConversationResponse(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public UUID getConversationId() { return conversationId; }
}
