package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import com.example.demo.dto.ChatMessageRequest;
import com.example.demo.dto.HostToolRequest;
import com.example.demo.dto.HostToolResponse;
import com.example.demo.model.ChatSession;
import com.example.demo.model.HostToolClientException;
import com.example.demo.model.Tenant;
import com.example.demo.model.TenantToolConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FlowelleToolClientTest {
    private final HostToolClient hostToolClient = mock(HostToolClient.class);
    private final FlowelleToolClient flowelleToolClient = new FlowelleToolClient(hostToolClient, new ObjectMapper());

    @Test
    void mapsCycleSummaryThroughTypedContract() {
        Tenant tenant = new Tenant("demo", "Demo Tenant");
        TenantToolConfig toolConfig = new TenantToolConfig(
                tenant,
                FlowelleToolClient.CYCLE_SUMMARY_TOOL,
                "https://flowelle.example/aif/tools/cycle-summary",
                "test-secret",
                Set.of("cycle:read"),
                true);
        ChatMessageRequest request = new ChatMessageRequest(
                "flowelle-user-1",
                null,
                "When is my next period?",
                "en-US",
                Set.of("cycle:read"));
        when(hostToolClient.invoke(any(TenantToolConfig.class), any(HostToolRequest.class)))
                .thenReturn(new HostToolResponse(
                        "cycle-summary",
                        "OK",
                        "Cycle prediction loaded.",
                        Map.of(
                                "nextPeriod", "2026-06-20",
                                "fertileWindowStart", "2026-06-01",
                                "fertileWindowEnd", "2026-06-07",
                                "ovulationDay", "2026-06-06",
                                "confidence", 82,
                                "basis", "latest-cycle",
                                "isPredicted", true,
                                "cycleLength", 28,
                                "periodLength", 5),
                        "Used Flowelle cycle data."));

        HostToolResponse response = flowelleToolClient.fetchCycleSummary(
                tenant,
                new ChatSession(tenant, "flowelle-user-1"),
                request,
                toolConfig,
                Set.of("cycle:read"));

        assertThat(response.summary()).isEqualTo("Cycle prediction loaded.");
        assertThat(response.userExplanation()).isEqualTo("Used Flowelle cycle data.");
        assertThat(response.facts()).containsEntry("nextPeriod", "2026-06-20");

        ArgumentCaptor<HostToolRequest> requestCaptor = ArgumentCaptor.captor();
        verify(hostToolClient).invoke(any(TenantToolConfig.class), requestCaptor.capture());
        assertThat(requestCaptor.getValue().parameters())
                .containsEntry("contractVersion", "flowelle.cycle-summary.v1")
                .containsEntry("externalUserId", "flowelle-user-1");
        assertThat(requestCaptor.getValue().parameters().values())
                .doesNotContain("When is my next period?");
    }

    @Test
    void mapsUserPreferencesThroughTypedContract() {
        Tenant tenant = new Tenant("demo", "Demo Tenant");
        TenantToolConfig toolConfig = new TenantToolConfig(
                tenant,
                FlowelleToolClient.USER_PREFERENCES_TOOL,
                "https://flowelle.example/aif/tools/user-preferences",
                "test-secret",
                Set.of("preferences:read"),
                true);
        ChatMessageRequest request = new ChatMessageRequest(
                "flowelle-user-1",
                null,
                "Suggest food and exercise",
                "en-US",
                Set.of("preferences:read"));
        when(hostToolClient.invoke(any(TenantToolConfig.class), any(HostToolRequest.class)))
                .thenReturn(new HostToolResponse(
                        "user-preferences",
                        "OK",
                        "Preferences loaded.",
                        Map.of(
                                "cycleLength", 30,
                                "periodLength", 4,
                                "birthControlUse", false,
                                "aiCoachEnabled", true),
                        "Used Flowelle preferences."));

        HostToolResponse response = flowelleToolClient.fetchUserPreferences(
                tenant,
                new ChatSession(tenant, "flowelle-user-1"),
                request,
                toolConfig,
                Set.of("preferences:read"));

        assertThat(response.summary()).isEqualTo("Preferences loaded.");
        assertThat(response.facts()).containsEntry("cycleLength", 30);
    }

    @Test
    void preservesNoDataCycleSummaryResponse() {
        Tenant tenant = new Tenant("demo", "Demo Tenant");
        TenantToolConfig toolConfig = new TenantToolConfig(
                tenant,
                FlowelleToolClient.CYCLE_SUMMARY_TOOL,
                "https://flowelle.example/aif/tools/cycle-summary",
                "test-secret",
                Set.of("cycle:read"),
                true);
        ChatMessageRequest request = new ChatMessageRequest(
                "flowelle-user-1",
                null,
                "When is my next period?",
                "en-US",
                Set.of("cycle:read"));
        when(hostToolClient.invoke(any(TenantToolConfig.class), any(HostToolRequest.class)))
                .thenReturn(new HostToolResponse(
                        "cycle-summary",
                        "NO_DATA",
                        "No cycle data is available yet.",
                        Map.of(),
                        "I could not find enough Flowelle cycle data."));

        HostToolResponse response = flowelleToolClient.fetchCycleSummary(
                tenant,
                new ChatSession(tenant, "flowelle-user-1"),
                request,
                toolConfig,
                Set.of("cycle:read"));

        assertThat(response.status()).isEqualTo("NO_DATA");
        assertThat(response.summary()).isEqualTo("No cycle data is available yet.");
        assertThat(response.facts()).isEmpty();
    }

    @Test
    void rejectsMalformedCycleSummaryResponse() {
        Tenant tenant = new Tenant("demo", "Demo Tenant");
        TenantToolConfig toolConfig = new TenantToolConfig(
                tenant,
                FlowelleToolClient.CYCLE_SUMMARY_TOOL,
                "https://flowelle.example/aif/tools/cycle-summary",
                "test-secret",
                Set.of("cycle:read"),
                true);
        ChatMessageRequest request = new ChatMessageRequest(
                "flowelle-user-1",
                null,
                "When is my next period?",
                "en-US",
                Set.of("cycle:read"));
        when(hostToolClient.invoke(any(TenantToolConfig.class), any(HostToolRequest.class)))
                .thenReturn(new HostToolResponse("cycle-summary", "OK", "No usable data.", Map.of("basis", "none"), null));

        assertThatThrownBy(() -> flowelleToolClient.fetchCycleSummary(
                tenant,
                new ChatSession(tenant, "flowelle-user-1"),
                request,
                toolConfig,
                Set.of("cycle:read")))
                .isInstanceOf(HostToolClientException.class)
                .hasMessageContaining("missing prediction and cycle length");
    }
}
