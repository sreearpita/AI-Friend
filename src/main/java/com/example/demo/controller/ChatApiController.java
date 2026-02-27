package com.example.demo.controller;

import com.example.demo.dto.ChatReply;
import com.example.demo.dto.ChatRequest;
import com.example.demo.service.ChatApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:3000")
public class ChatApiController {

    private final ChatApiService chatApiService;

    public ChatApiController(ChatApiService chatApiService) {
        this.chatApiService = chatApiService;
    }

    @PostMapping
    public ResponseEntity<ChatReply> chat(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody ChatRequest request) {
        String reply = chatApiService.chat(
                userId,
                UUID.fromString(request.getConversationId()),
                request.getMessage());
        return ResponseEntity.ok(new ChatReply(reply));
    }
}
