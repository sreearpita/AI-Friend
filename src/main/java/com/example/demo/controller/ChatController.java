package com.example.demo.controller;

import com.example.demo.dto.ChatMessageRequest;
import com.example.demo.dto.ChatMessageResponse;
import com.example.demo.model.Tenant;
import com.example.demo.security.TenantAuthInterceptor;
import com.example.demo.security.TenantAuthService;
import com.example.demo.service.ChatOrchestratorService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatOrchestratorService chatOrchestratorService;
    private final TenantAuthService tenantAuthService;

    public ChatController(ChatOrchestratorService chatOrchestratorService, TenantAuthService tenantAuthService) {
        this.chatOrchestratorService = chatOrchestratorService;
        this.tenantAuthService = tenantAuthService;
    }

    @PostMapping("/v1/chat/messages")
    public ChatMessageResponse chat(
            @RequestAttribute(TenantAuthInterceptor.TENANT_ATTRIBUTE) Tenant tenant,
            @Valid @RequestBody ChatMessageRequest request) {
        logger.info("Received chat request. tenant={} externalUserId={} hasSession={}",
                tenant.getSlug(),
                request.externalUserId(),
                request.sessionId() != null);
        return chatOrchestratorService.chat(tenant, request);
    }

    @PostMapping(value = "/chat", consumes = MediaType.TEXT_PLAIN_VALUE)
    public String legacyChat(@RequestBody String message) {
        logger.info("Received legacy chat request.");
        Tenant demoTenant = tenantAuthService.requireDemoTenant();
        ChatMessageRequest request = new ChatMessageRequest(
                "legacy-demo-user",
                null,
                message,
                "en-US",
                java.util.Set.of("legacy"));
        return chatOrchestratorService.chat(demoTenant, request).answer();
    }
}
