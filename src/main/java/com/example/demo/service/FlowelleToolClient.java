package com.example.demo.service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.example.demo.dto.ChatMessageRequest;
import com.example.demo.dto.FlowelleCycleSummaryRequest;
import com.example.demo.dto.FlowelleCycleSummaryResponse;
import com.example.demo.dto.FlowelleUserPreferencesRequest;
import com.example.demo.dto.FlowelleUserPreferencesResponse;
import com.example.demo.dto.HostToolRequest;
import com.example.demo.dto.HostToolResponse;
import com.example.demo.model.ChatSession;
import com.example.demo.model.HostToolClientException;
import com.example.demo.model.Tenant;
import com.example.demo.model.TenantToolConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FlowelleToolClient {
    public static final String CYCLE_SUMMARY_TOOL = "cycle-summary";
    public static final String USER_PREFERENCES_TOOL = "user-preferences";
    private static final String CYCLE_SUMMARY_CONTRACT = "flowelle.cycle-summary.v1";
    private static final String USER_PREFERENCES_CONTRACT = "flowelle.user-preferences.v1";

    private final HostToolClient hostToolClient;
    private final ObjectMapper objectMapper;
    private final TypeReference<Map<String, Object>> mapTypeReference = new TypeReference<>() {
    };

    public FlowelleToolClient(HostToolClient hostToolClient, ObjectMapper objectMapper) {
        this.hostToolClient = hostToolClient;
        this.objectMapper = objectMapper;
    }

    public HostToolResponse fetchCycleSummary(
            Tenant tenant,
            ChatSession session,
            ChatMessageRequest request,
            TenantToolConfig toolConfig,
            Set<String> requestScopes) {
        UUID requestId = UUID.randomUUID();
        FlowelleCycleSummaryRequest flowelleRequest = new FlowelleCycleSummaryRequest(
                requestId,
                request.externalUserId(),
                session.getId(),
                requestScopes,
                localeOrDefault(request.locale()),
                CYCLE_SUMMARY_CONTRACT);
        HostToolResponse response = invoke(tenant, session, toolConfig, requestScopes, requestId, flowelleRequest);
        FlowelleCycleSummaryResponse typedResponse = convertFacts(response, FlowelleCycleSummaryResponse.class);
        validateCycleSummary(typedResponse);
        return new HostToolResponse(
                CYCLE_SUMMARY_TOOL,
                response.status(),
                firstText(typedResponse.summary(), response.summary(), "Flowelle cycle summary returned."),
                objectMapper.convertValue(typedResponse, mapTypeReference),
                firstText(typedResponse.userExplanation(), response.userExplanation(), "Used Flowelle cycle summary."));
    }

    public HostToolResponse fetchUserPreferences(
            Tenant tenant,
            ChatSession session,
            ChatMessageRequest request,
            TenantToolConfig toolConfig,
            Set<String> requestScopes) {
        UUID requestId = UUID.randomUUID();
        FlowelleUserPreferencesRequest flowelleRequest = new FlowelleUserPreferencesRequest(
                requestId,
                request.externalUserId(),
                session.getId(),
                requestScopes,
                localeOrDefault(request.locale()),
                USER_PREFERENCES_CONTRACT);
        HostToolResponse response = invoke(tenant, session, toolConfig, requestScopes, requestId, flowelleRequest);
        FlowelleUserPreferencesResponse typedResponse = convertFacts(response, FlowelleUserPreferencesResponse.class);
        validateUserPreferences(typedResponse);
        return new HostToolResponse(
                USER_PREFERENCES_TOOL,
                response.status(),
                firstText(typedResponse.summary(), response.summary(), "Flowelle preferences returned."),
                objectMapper.convertValue(typedResponse, mapTypeReference),
                firstText(typedResponse.userExplanation(), response.userExplanation(), "Used Flowelle preferences."));
    }

    private HostToolResponse invoke(
            Tenant tenant,
            ChatSession session,
            TenantToolConfig toolConfig,
            Set<String> requestScopes,
            UUID requestId,
            Object flowelleRequest) {
        HostToolRequest hostToolRequest = new HostToolRequest(
                requestId,
                tenant.getSlug(),
                externalUserId(flowelleRequest),
                session.getId(),
                toolConfig.getName(),
                requestScopes,
                locale(flowelleRequest),
                objectMapper.convertValue(flowelleRequest, mapTypeReference));
        HostToolResponse response = hostToolClient.invoke(toolConfig, hostToolRequest);
        if (response == null || response.facts() == null) {
            throw new HostToolClientException("Flowelle tool returned no usable response", null);
        }
        return response;
    }

    private <T> T convertFacts(HostToolResponse response, Class<T> responseType) {
        try {
            return objectMapper.convertValue(response.facts(), responseType);
        } catch (IllegalArgumentException exception) {
            throw new HostToolClientException("Flowelle tool response did not match the expected contract", exception);
        }
    }

    private void validateCycleSummary(FlowelleCycleSummaryResponse response) {
        if (!StringUtils.hasText(response.nextPeriod()) && response.cycleLength() == null) {
            throw new HostToolClientException("Flowelle cycle summary response is missing prediction and cycle length", null);
        }
    }

    private void validateUserPreferences(FlowelleUserPreferencesResponse response) {
        if (response.cycleLength() == null && response.periodLength() == null && response.birthControlUse() == null) {
            throw new HostToolClientException("Flowelle user preferences response is missing preference fields", null);
        }
    }

    private String localeOrDefault(String locale) {
        return StringUtils.hasText(locale) ? locale : "en-US";
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

    private String externalUserId(Object request) {
        if (request instanceof FlowelleCycleSummaryRequest cycleSummaryRequest) {
            return cycleSummaryRequest.externalUserId();
        }
        return ((FlowelleUserPreferencesRequest) request).externalUserId();
    }

    private String locale(Object request) {
        if (request instanceof FlowelleCycleSummaryRequest cycleSummaryRequest) {
            return cycleSummaryRequest.locale();
        }
        return ((FlowelleUserPreferencesRequest) request).locale();
    }
}
