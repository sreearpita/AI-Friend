package com.example.demo.controller;

import com.example.demo.dto.ConversationResponse;
import com.example.demo.model.AppUser;
import com.example.demo.model.Conversation;
import com.example.demo.service.ConversationService;
import com.example.demo.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
public class ConversationController {

    private final UserService userService;
    private final ConversationService conversationService;

    public ConversationController(UserService userService, ConversationService conversationService) {
        this.userService = userService;
        this.conversationService = conversationService;
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> create(
            @RequestHeader("X-User-Id") UUID userId) {
        AppUser user = userService.getOrCreate(userId);
        Conversation conversation = conversationService.create(user);
        return ResponseEntity.ok(new ConversationResponse(conversation.getId()));
    }
}
