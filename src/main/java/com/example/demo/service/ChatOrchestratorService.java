package com.example.demo.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.example.demo.config.AiFriendProperties;
import com.example.demo.dto.ChatMessageRequest;
import com.example.demo.dto.ChatMessageResponse;
import com.example.demo.dto.CitationResponse;
import com.example.demo.dto.ToolCallResponse;
import com.example.demo.exception.ApiException;
import com.example.demo.model.ChatMessage;
import com.example.demo.model.ChatPromptMessage;
import com.example.demo.model.ChatSession;
import com.example.demo.model.MessageRole;
import com.example.demo.model.ModelClient;
import com.example.demo.model.ModelClientException;
import com.example.demo.model.SafetyDecision;
import com.example.demo.model.SafetyStatus;
import com.example.demo.model.Tenant;
import com.example.demo.model.ToolExecutionResult;
import com.example.demo.repository.ChatMessageRepository;
import com.example.demo.repository.ChatSessionRepository;
import com.example.demo.repository.TenantRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ChatOrchestratorService {
    private static final Logger logger = LoggerFactory.getLogger(ChatOrchestratorService.class);

    private static final String SYSTEM_PROMPT = """
            You are AI-Friend, an empathetic wellness assistant embedded into host applications.
            You can be warm and conversational, but you must stay inside wellness education and support.
            Do not diagnose, prescribe, promise medical outcomes, or replace a clinician.
            If a user asks about menstrual or hormonal health, explain uncertainty and encourage professional care for urgent, severe, persistent, or worrying symptoms.
            Use host-app facts only when they are explicitly supplied through approved tools or context.
            """;

    private final AiFriendProperties properties;
    private final TenantRepository tenantRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ModelClient modelClient;
    private final SafetyService safetyService;
    private final RetrievalService retrievalService;
    private final ToolRegistryService toolRegistryService;
    private final AuditService auditService;

    public ChatOrchestratorService(
            AiFriendProperties properties,
            TenantRepository tenantRepository,
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            ModelClient modelClient,
            SafetyService safetyService,
            RetrievalService retrievalService,
            ToolRegistryService toolRegistryService,
            AuditService auditService) {
        this.properties = properties;
        this.tenantRepository = tenantRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.modelClient = modelClient;
        this.safetyService = safetyService;
        this.retrievalService = retrievalService;
        this.toolRegistryService = toolRegistryService;
        this.auditService = auditService;
    }

    @Transactional
    public ChatMessageResponse chat(Tenant authenticatedTenant, ChatMessageRequest request) {
        validateRequest(request);

        Tenant tenant = tenantRepository.getReferenceById(authenticatedTenant.getId());
        ChatSession session = resolveSession(tenant, request);
        SafetyDecision safetyDecision = safetyService.evaluate(request.message());

        auditService.record(tenant, request.externalUserId(), session.getId(), "chat.message.received", Map.of(
                "messageLength", request.message().length(),
                "locale", request.locale() == null ? "" : request.locale(),
                "scopeCount", request.scopes() == null ? 0 : request.scopes().size()));

        chatMessageRepository.save(new ChatMessage(
                tenant,
                session,
                MessageRole.USER,
                request.message(),
                safetyDecision.status()));

        List<CitationResponse> citations = List.of();
        List<String> citationPromptContexts = List.of();
        ToolExecutionResult toolExecutionResult = ToolExecutionResult.empty();
        if (safetyDecision.shouldCallModel()) {
            citations = retrievalService.findRelevantCitations(request.message());
            citationPromptContexts = retrievalService.findPromptContexts(request.message());
            toolExecutionResult = toolRegistryService.executeTools(tenant, session, request);
        }
        List<ToolCallResponse> toolCalls = toolExecutionResult.toolCalls();

        String answer;
        SafetyStatus finalStatus = safetyDecision.status();
        if (safetyDecision.shouldCallModel()) {
            try {
                answer = modelClient.generate(buildPrompt(session, citationPromptContexts, toolExecutionResult.promptContexts()));
            } catch (ModelClientException exception) {
                logger.warn("Model call failed. tenant={} sessionId={} reason={}",
                        authenticatedTenant.getSlug(),
                        session.getId(),
                        exception.getMessage());
                answer = safetyService.modelFallbackResponse();
                finalStatus = SafetyStatus.MODEL_FALLBACK;
            }
        } else {
            answer = safetyDecision.directResponse();
        }

        chatMessageRepository.save(new ChatMessage(
                tenant,
                session,
                MessageRole.ASSISTANT,
                answer,
                finalStatus));

        auditService.record(tenant, request.externalUserId(), session.getId(), "chat.message.completed", Map.of(
                "answerLength", answer.length(),
                "safetyStatus", finalStatus.name(),
                "citationCount", citations.size(),
                "toolCallCount", toolCalls.size(),
                "toolStatuses", toolCalls.stream()
                        .map(toolCall -> toolCall.name() + ":" + toolCall.status())
                        .toList()));

        return new ChatMessageResponse(
                session.getId(),
                answer,
                finalStatus,
                citations,
                toolCalls,
                Instant.now());
    }

    private ChatSession resolveSession(Tenant tenant, ChatMessageRequest request) {
        if (request.sessionId() == null) {
            return chatSessionRepository.save(new ChatSession(tenant, request.externalUserId()));
        }

        ChatSession session = chatSessionRepository.findByIdAndTenantId(request.sessionId(), tenant.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Chat session was not found."));
        if (!session.getExternalUserId().equals(request.externalUserId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SESSION_USER_MISMATCH", "Session does not belong to this user.");
        }
        return session;
    }

    private List<ChatPromptMessage> buildPrompt(
            ChatSession session,
            List<String> citationPromptContexts,
            List<String> toolPromptContexts) {
        List<ChatPromptMessage> messages = new ArrayList<>();
        messages.add(new ChatPromptMessage("system", SYSTEM_PROMPT));

        if (StringUtils.hasText(session.getDerivedPreferenceSummary())) {
            messages.add(new ChatPromptMessage(
                    "system",
                    "Known user preference summary: " + session.getDerivedPreferenceSummary()));
        }

        if (!citationPromptContexts.isEmpty()) {
            messages.add(new ChatPromptMessage(
                    "system",
                    "Curated wellness references for this turn. Use when relevant and do not overstate them:\n"
                            + String.join("\n\n", citationPromptContexts)));
        }

        if (!toolPromptContexts.isEmpty()) {
            messages.add(new ChatPromptMessage(
                    "system",
                    "Approved host-app facts for this turn:\n" + String.join("\n", toolPromptContexts)));
        }

        List<ChatMessage> history = chatMessageRepository.findBySessionIdOrderByCreatedAtDesc(
                session.getId(),
                PageRequest.of(0, properties.getChat().getMaxHistoryMessages()));
        Collections.reverse(history);
        for (ChatMessage historyMessage : history) {
            String role = historyMessage.getRole() == MessageRole.ASSISTANT ? "assistant" : "user";
            messages.add(new ChatPromptMessage(role, historyMessage.getContent()));
        }

        return messages;
    }

    private void validateRequest(ChatMessageRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request body is required.");
        }
        if (!StringUtils.hasText(request.externalUserId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "externalUserId must not be blank.");
        }
        if (!StringUtils.hasText(request.message())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "message must not be blank.");
        }
        if (request.message().length() > properties.getChat().getMaxMessageLength()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "message is too long.");
        }
    }
}
