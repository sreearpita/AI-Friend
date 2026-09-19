package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.example.demo.model.ChatCommand;
import com.example.demo.model.ChatSession;
import com.example.demo.model.Tenant;
import com.example.demo.model.TenantToolConfig;
import com.example.demo.repository.TenantToolConfigRepository;

class ToolRegistryConsentTest {
    private final TenantToolConfigRepository configRepository = mock(TenantToolConfigRepository.class);
    private final FlowelleToolClient flowelleToolClient = mock(FlowelleToolClient.class);
    private final PlatformMetrics platformMetrics = mock(PlatformMetrics.class);
    private final ToolRegistryService registry = new ToolRegistryService(
            configRepository,
            flowelleToolClient,
            platformMetrics);

    @Test
    void skipsDetectedFlowelleToolsWithoutInvokingHostWhenConsentIsDisabled() {
        Tenant tenant = new Tenant("demo", "Demo Tenant");
        TenantToolConfig config = new TenantToolConfig(
                tenant,
                FlowelleToolClient.CYCLE_SUMMARY_TOOL,
                "https://flowelle.example/aif/tools/cycle-summary",
                "secret",
                Set.of("cycle:read"),
                true);
        when(configRepository.findByTenantIdAndNameAndActiveTrue(
                any(),
                org.mockito.ArgumentMatchers.eq(FlowelleToolClient.CYCLE_SUMMARY_TOOL)))
                .thenReturn(java.util.Optional.of(config));

        ChatCommand command = new ChatCommand(
                UUID.randomUUID(),
                "42",
                null,
                "When is my next period?",
                "en-US",
                Set.of("cycle:read"),
                "jti-42",
                false);

        var result = registry.executeTools(tenant, new ChatSession(tenant, "42"), command);

        assertThat(result.toolCalls()).singleElement().satisfies(toolCall -> {
            assertThat(toolCall.name()).isEqualTo(FlowelleToolClient.CYCLE_SUMMARY_TOOL);
            assertThat(toolCall.status()).isEqualTo("SKIPPED");
            assertThat(toolCall.summary()).contains("consent");
        });
        assertThat(result.promptContexts()).isEmpty();
        verify(flowelleToolClient, never()).fetchCycleSummary(
                any(),
                any(),
                any(com.example.demo.model.ChatCommand.class),
                any(),
                any());
    }
}
