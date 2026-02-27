package com.example.demo.controller;

import com.example.demo.dto.ChatReply;
import com.example.demo.dto.ChatRequest;
import com.example.demo.service.ChatApiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
public class ChatApiController {

    private final ChatApiService chatApiService;

    public ChatApiController(ChatApiService chatApiService) {
        this.chatApiService = chatApiService;
    }

    @PostMapping
    public ResponseEntity<ChatReply> chat(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody ChatRequest request) {

        UUID conversationId;
        try {
            conversationId = UUID.fromString(request.getConversationId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        try {
            String reply = chatApiService.chat(userId, conversationId, request.getMessage());
            return ResponseEntity.ok(new ChatReply(reply));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        }
    }
}
