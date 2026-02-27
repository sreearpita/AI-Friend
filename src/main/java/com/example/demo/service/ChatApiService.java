package com.example.demo.service;

import com.example.demo.model.AppUser;
import com.example.demo.model.Conversation;
import com.example.demo.model.Memory;
import com.example.demo.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChatApiService {

    private static final Logger logger = LoggerFactory.getLogger(ChatApiService.class);

    private final UserService userService;
    private final ConversationService conversationService;
    private final MemoryService memoryService;
    private final OllamaService ollamaService;

    @Value("${app.persona}")
    private String personaPrompt;

    @Value("${app.chat.last-n-messages:20}")
    private int lastNMessages;

    @Value("${app.memory.top-k:10}")
    private int topKMemories;

    public ChatApiService(UserService userService,
                          ConversationService conversationService,
                          MemoryService memoryService,
                          OllamaService ollamaService) {
        this.userService = userService;
        this.conversationService = conversationService;
        this.memoryService = memoryService;
        this.ollamaService = ollamaService;
    }

    @Transactional
    public String chat(UUID userId, UUID conversationId, String userMessage) {
        AppUser user = userService.getOrCreate(userId);

        Conversation conversation = conversationService.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        List<Memory> memories = memoryService.getTopMemories(userId, topKMemories);
        List<Message> history = conversationService.getRecentMessages(conversationId, lastNMessages);

        List<OllamaService.OllamaMessage> promptMessages = buildPrompt(memories, history, userMessage);

        logger.debug("Calling LLM for user={} conversation={}", userId, conversationId);
        String aiReply;
        try {
            aiReply = ollamaService.chat(promptMessages);
        } catch (Exception e) {
            logger.error("LLM call failed", e);
            throw new RuntimeException("AI service is unavailable, please try again later.", e);
        }

        conversationService.addMessage(conversation, "user", userMessage);
        conversationService.addMessage(conversation, "assistant", aiReply);

        memoryService.extractAndSave(user, userMessage);

        return aiReply;
    }

    private List<OllamaService.OllamaMessage> buildPrompt(
            List<Memory> memories,
            List<Message> history,
            String currentMessage) {

        List<OllamaService.OllamaMessage> messages = new ArrayList<>();
        messages.add(new OllamaService.OllamaMessage("system", buildSystemPrompt(memories)));

        for (Message m : history) {
            messages.add(new OllamaService.OllamaMessage(m.getRole(), m.getContent()));
        }

        messages.add(new OllamaService.OllamaMessage("user", currentMessage));
        return messages;
    }

    private String buildSystemPrompt(List<Memory> memories) {
        if (memories.isEmpty()) {
            return personaPrompt;
        }
        StringBuilder sb = new StringBuilder(personaPrompt);
        sb.append("\n\nHere are some things you remember about the user:\n");
        for (Memory m : memories) {
            sb.append("- ").append(m.getFact()).append("\n");
        }
        return sb.toString();
    }
}

