package com.example.demo.controller;

import com.example.demo.dto.ChatMessageRequest;
import com.example.demo.dto.ChatMessageResponse;
import com.example.demo.dto.ChatV2MessageRequest;
import com.example.demo.model.AuthenticatedUserContext;
import com.example.demo.model.ChatCommand;
import com.example.demo.model.Tenant;
import com.example.demo.security.TenantAuthInterceptor;
import com.example.demo.security.UserContextAuthService;
import com.example.demo.service.ChatOrchestratorService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    public static final String USER_CONTEXT_HEADER = "X-AIF-User-Context";

    private final ChatOrchestratorService chatOrchestratorService;
    private final UserContextAuthService userContextAuthService;

    public ChatController(ChatOrchestratorService chatOrchestratorService, UserContextAuthService userContextAuthService) {
        this.chatOrchestratorService = chatOrchestratorService;
        this.userContextAuthService = userContextAuthService;
    }

    @PostMapping("/v2/chat/messages")
    public ChatMessageResponse chatV2(
            @RequestAttribute(TenantAuthInterceptor.TENANT_ATTRIBUTE) Tenant tenant,
            @RequestHeader(name = USER_CONTEXT_HEADER, required = false) String userContextJwt,
            @Valid @RequestBody ChatV2MessageRequest request) {
        AuthenticatedUserContext userContext = userContextAuthService.authenticate(tenant, userContextJwt);
        logger.info("Received v2 chat request. tenant={} externalUserId={} hasSession={} authJti={}",
                tenant.getSlug(),
                userContext.externalUserId(),
                request.sessionId() != null,
                userContext.jwtId());
        return chatOrchestratorService.chat(tenant, ChatCommand.fromV2(request, userContext));
    }
}

@RestController
@Profile("local")
class LocalChatController {
    private static final Logger logger = LoggerFactory.getLogger(LocalChatController.class);

    private final ChatOrchestratorService chatOrchestratorService;
    private final com.example.demo.security.TenantAuthService tenantAuthService;

    LocalChatController(
            ChatOrchestratorService chatOrchestratorService,
            com.example.demo.security.TenantAuthService tenantAuthService) {
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
        return chatOrchestratorService.chat(tenant, ChatCommand.fromV1(request));
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
        return chatOrchestratorService.chat(demoTenant, ChatCommand.fromV1(request)).answer();
    }
}
