package com.example.demo.dto;

import java.util.UUID;

public class ConversationResponse {
    private UUID conversationId;

    public ConversationResponse() {}

    public ConversationResponse(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
}
