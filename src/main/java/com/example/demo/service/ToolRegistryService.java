package com.example.demo.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.demo.dto.HostToolResponse;
import com.example.demo.dto.ToolCallResponse;
import com.example.demo.model.ChatCommand;
import com.example.demo.model.ChatSession;
import com.example.demo.model.HostToolClientException;
import com.example.demo.model.Tenant;
import com.example.demo.model.TenantToolConfig;
import com.example.demo.model.ToolExecutionResult;
import com.example.demo.repository.TenantToolConfigRepository;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ToolRegistryService {
    private final TenantToolConfigRepository tenantToolConfigRepository;
    private final FlowelleToolClient flowelleToolClient;
    private final PlatformMetrics platformMetrics;

    public ToolRegistryService(
            TenantToolConfigRepository tenantToolConfigRepository,
            FlowelleToolClient flowelleToolClient,
            PlatformMetrics platformMetrics) {
        this.tenantToolConfigRepository = tenantToolConfigRepository;
        this.flowelleToolClient = flowelleToolClient;
        this.platformMetrics = platformMetrics;
    }

    public ToolExecutionResult executeTools(Tenant tenant, ChatSession session, ChatCommand command) {
        Set<String> toolNames = detectToolNames(command.message());
        if (toolNames.isEmpty()) {
            return ToolExecutionResult.empty();
        }

        List<ToolCallResponse> toolCalls = new ArrayList<>();
        List<String> promptContexts = new ArrayList<>();
        Set<String> requestScopes = normalizeScopes(command.scopes());

        for (String toolName : toolNames) {
            if (!command.aiCoachEnabled()) {
                toolCalls.add(new ToolCallResponse(
                        toolName,
                        "SKIPPED",
                        "Flowelle data is unavailable because AI coaching consent is not enabled."));
                platformMetrics.recordToolOutcome(tenant.getSlug(), toolName, "SKIPPED");
                continue;
            }
            tenantToolConfigRepository.findByTenantIdAndNameAndActiveTrue(tenant.getId(), toolName)
                    .ifPresentOrElse(
                            toolConfig -> invokeConfiguredTool(
                                    tenant,
                                    session,
                                    command,
                                    requestScopes,
                                    toolConfig,
                                    toolCalls,
                                    promptContexts),
                            () -> {
                                toolCalls.add(new ToolCallResponse(
                                        toolName,
                                        "SKIPPED",
                                        "Host tool is not configured for this tenant."));
                                platformMetrics.recordToolOutcome(tenant.getSlug(), toolName, "SKIPPED");
                            });
        }

        return new ToolExecutionResult(List.copyOf(toolCalls), List.copyOf(promptContexts));
    }

    private void invokeConfiguredTool(
            Tenant tenant,
            ChatSession session,
            ChatCommand command,
            Set<String> requestScopes,
            TenantToolConfig toolConfig,
            List<ToolCallResponse> toolCalls,
            List<String> promptContexts) {
        if (!isScopeAllowed(requestScopes, normalizeScopes(toolConfig.getAllowedScopes()))) {
            toolCalls.add(new ToolCallResponse(
                    toolConfig.getName(),
                    "SKIPPED",
                    "Required host-app scope is not available for this request."));
            platformMetrics.recordToolOutcome(tenant.getSlug(), toolConfig.getName(), "SKIPPED");
            return;
        }

        try {
            HostToolResponse response = invokeFlowelleTool(tenant, session, command, requestScopes, toolConfig);
            if (response == null) {
                toolCalls.add(new ToolCallResponse(
                        toolConfig.getName(),
                        "FAILED",
                        "Host tool returned no usable response."));
                platformMetrics.recordToolOutcome(tenant.getSlug(), toolConfig.getName(), "FAILED");
                return;
            }
            String summary = firstText(response.userExplanation(), response.summary(), "Host tool returned context.");
            if (isSuccess(response.status())) {
                toolCalls.add(new ToolCallResponse(toolConfig.getName(), "COMPLETED", summary));
                platformMetrics.recordToolOutcome(tenant.getSlug(), toolConfig.getName(), "COMPLETED");
                promptContexts.add(promptContext(toolConfig.getName(), response));
            } else {
                toolCalls.add(new ToolCallResponse(toolConfig.getName(), "FAILED", summary));
                platformMetrics.recordToolOutcome(tenant.getSlug(), toolConfig.getName(), "FAILED");
            }
        } catch (HostToolClientException exception) {
            toolCalls.add(new ToolCallResponse(
                    toolConfig.getName(),
                    "FAILED",
                    "Host tool was unavailable; answering with general wellness guidance."));
            platformMetrics.recordToolOutcome(tenant.getSlug(), toolConfig.getName(), "FAILED");
        }
    }

    private Set<String> detectToolNames(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        Set<String> toolNames = new LinkedHashSet<>();
        if (normalized.contains("next period")
                || normalized.contains("period date")
                || normalized.contains("cycle length")
                || normalized.contains("cycle stats")) {
            toolNames.add(FlowelleToolClient.CYCLE_SUMMARY_TOOL);
        }
        if (normalized.contains("food")
                || normalized.contains("diet")
                || normalized.contains("exercise")
                || normalized.contains("preference")
                || normalized.contains("lifestyle")) {
            toolNames.add(FlowelleToolClient.USER_PREFERENCES_TOOL);
        }
        return toolNames;
    }

    private Set<String> normalizeScopes(Set<String> scopes) {
        if (scopes == null) {
            return Set.of();
        }
        return scopes.stream()
                .filter(StringUtils::hasText)
                .map(scope -> scope.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    private boolean isScopeAllowed(Set<String> requestScopes, Set<String> allowedScopes) {
        if (allowedScopes.isEmpty()) {
            return false;
        }
        return requestScopes.stream().anyMatch(allowedScopes::contains);
    }

    private HostToolResponse invokeFlowelleTool(
            Tenant tenant,
            ChatSession session,
            ChatCommand command,
            Set<String> requestScopes,
            TenantToolConfig toolConfig) {
        return switch (toolConfig.getName()) {
            case FlowelleToolClient.CYCLE_SUMMARY_TOOL ->
                    flowelleToolClient.fetchCycleSummary(tenant, session, command, toolConfig, requestScopes);
            case FlowelleToolClient.USER_PREFERENCES_TOOL ->
                    flowelleToolClient.fetchUserPreferences(tenant, session, command, toolConfig, requestScopes);
            default -> throw new HostToolClientException("Unsupported Flowelle tool: " + toolConfig.getName(), null);
        };
    }

    private boolean isSuccess(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        String normalized = status.toUpperCase(Locale.ROOT);
        return "OK".equals(normalized) || "SUCCESS".equals(normalized) || "COMPLETED".equals(normalized);
    }

    private String firstText(String first, String second, String fallback) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        if (StringUtils.hasText(second)) {
            return second;
        }
        return fallback;
    }

    private String promptContext(String toolName, HostToolResponse response) {
        return "Host tool " + toolName + " returned summary: "
                + firstText(response.summary(), response.userExplanation(), "No summary provided.")
                + " Facts: "
                + (response.facts() == null ? Map.of() : response.facts());
    }
}
