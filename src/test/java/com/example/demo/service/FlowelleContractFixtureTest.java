package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import com.example.demo.dto.ChatMessageRequest;
import com.example.demo.dto.HostToolRequest;
import com.example.demo.dto.HostToolResponse;
import com.example.demo.model.ChatSession;
import com.example.demo.model.Tenant;
import com.example.demo.model.TenantToolConfig;
import com.example.demo.support.ContractFixtures;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FlowelleContractFixtureTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HostToolClient hostToolClient = mock(HostToolClient.class);
    private final FlowelleToolClient flowelleToolClient = new FlowelleToolClient(hostToolClient, objectMapper);

    @Test
    void parsesCycleSummaryResponseFixture() throws Exception {
        HostToolResponse fixtureResponse = objectMapper.readValue(
                ContractFixtures.read("/contracts/flowelle/cycle-summary-response.json"),
                HostToolResponse.class);
        when(hostToolClient.invoke(any(TenantToolConfig.class), any(HostToolRequest.class)))
                .thenReturn(fixtureResponse);

        HostToolResponse response = flowelleToolClient.fetchCycleSummary(
                tenant(),
                session(),
                chatRequest("When is my next period?", Set.of("cycle:read")),
                cycleSummaryConfig(),
                Set.of("cycle:read"));

        assertThat(response.status()).isEqualTo("OK");
        assertThat(response.facts()).containsEntry("nextPeriod", "2026-06-20");
        assertThat(response.userExplanation()).isEqualTo("I used your Flowelle cycle summary.");
    }

    @Test
    void buildsCycleSummaryRequestMatchingFixtureShape() throws Exception {
        JsonNode fixture = objectMapper.readTree(ContractFixtures.read("/contracts/flowelle/cycle-summary-request.json"));
        when(hostToolClient.invoke(any(TenantToolConfig.class), any(HostToolRequest.class)))
                .thenReturn(objectMapper.readValue(
                        ContractFixtures.read("/contracts/flowelle/cycle-summary-response.json"),
                        HostToolResponse.class));

        flowelleToolClient.fetchCycleSummary(
                tenant(),
                session(),
                chatRequest("When is my next period?", Set.of("cycle:read")),
                cycleSummaryConfig(),
                Set.of("cycle:read"));

        ArgumentCaptor<HostToolRequest> requestCaptor = ArgumentCaptor.forClass(HostToolRequest.class);
        org.mockito.Mockito.verify(hostToolClient).invoke(any(TenantToolConfig.class), requestCaptor.capture());

        HostToolRequest request = requestCaptor.getValue();
        assertThat(request.tenantSlug()).isEqualTo(fixture.get("tenantSlug").asText());
        assertThat(request.externalUserId()).isEqualTo(fixture.get("externalUserId").asText());
        assertThat(request.toolName()).isEqualTo(fixture.get("toolName").asText());
        assertThat(request.parameters())
                .containsEntry("contractVersion", fixture.at("/parameters/contractVersion").asText())
                .containsEntry("externalUserId", fixture.at("/parameters/externalUserId").asText());
    }

    @Test
    void parsesUserPreferencesResponseFixture() throws Exception {
        when(hostToolClient.invoke(any(TenantToolConfig.class), any(HostToolRequest.class)))
                .thenReturn(objectMapper.readValue(
                        ContractFixtures.read("/contracts/flowelle/user-preferences-response.json"),
                        HostToolResponse.class));

        HostToolResponse response = flowelleToolClient.fetchUserPreferences(
                tenant(),
                session(),
                chatRequest("Suggest food and exercise", Set.of("preferences:read")),
                userPreferencesConfig(),
                Set.of("preferences:read"));

        assertThat(response.status()).isEqualTo("OK");
        assertThat(response.facts()).containsEntry("cycleLength", 28);
        assertThat(response.facts()).containsEntry("notificationsEnabled", true);
    }

    @Test
    void preservesNoDataCycleSummaryWithoutTreatingItAsMalformed() throws Exception {
        when(hostToolClient.invoke(any(TenantToolConfig.class), any(HostToolRequest.class)))
                .thenReturn(objectMapper.readValue(
                        ContractFixtures.read("/contracts/flowelle/cycle-summary-no-data-response.json"),
                        HostToolResponse.class));

        HostToolResponse response = flowelleToolClient.fetchCycleSummary(
                tenant(),
                session(),
                chatRequest("When is my next period?", Set.of("cycle:read")),
                cycleSummaryConfig(),
                Set.of("cycle:read"));

        assertThat(response.status()).isEqualTo("NO_DATA");
        assertThat(response.summary()).isEqualTo("No cycle data is available yet.");
        assertThat(response.facts()).isEmpty();
    }

    private Tenant tenant() {
        return new Tenant("demo", "Demo Tenant");
    }

    private ChatSession session() {
        return new ChatSession(tenant(), "flowelle-user-1");
    }

    private ChatMessageRequest chatRequest(String message, Set<String> scopes) {
        return new ChatMessageRequest("flowelle-user-1", null, message, "en-US", scopes);
    }

    private TenantToolConfig cycleSummaryConfig() {
        return new TenantToolConfig(
                tenant(),
                FlowelleToolClient.CYCLE_SUMMARY_TOOL,
                "https://flowelle.example/api/aif/tools/cycle-summary",
                "test-secret",
                Set.of("cycle:read"),
                true);
    }

    private TenantToolConfig userPreferencesConfig() {
        return new TenantToolConfig(
                tenant(),
                FlowelleToolClient.USER_PREFERENCES_TOOL,
                "https://flowelle.example/aif/tools/user-preferences",
                "test-secret",
                Set.of("preferences:read"),
                true);
    }
}
