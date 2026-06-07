package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.example.demo.config.AiFriendProperties;
import com.example.demo.dto.HostToolRequest;
import com.example.demo.dto.HostToolResponse;
import com.example.demo.model.Tenant;
import com.example.demo.model.TenantToolConfig;
import com.example.demo.security.HostToolSigner;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class RestTemplateHostToolClientTest {
    @Test
    void sendsSignedCallbackRequest() {
        AiFriendProperties properties = new AiFriendProperties();
        ObjectMapper objectMapper = new ObjectMapper();
        HostToolSigner signer = new HostToolSigner();
        RestTemplateHostToolClient client = new RestTemplateHostToolClient(
                new RestTemplateBuilder(),
                properties,
                objectMapper,
                signer);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        Tenant tenant = new Tenant("demo", "Demo Tenant");
        TenantToolConfig toolConfig = new TenantToolConfig(
                tenant,
                "cycle-summary",
                "https://flowelle.example/aif/tools/cycle-summary",
                "test-secret",
                Set.of("cycle:read"),
                true);
        HostToolRequest request = new HostToolRequest(
                UUID.randomUUID(),
                "demo",
                "flowelle-user-1",
                UUID.randomUUID(),
                "cycle-summary",
                Set.of("cycle:read"),
                "en-US",
                Map.of("intent", "cycle-summary"));

        server.expect(requestTo("https://flowelle.example/aif/tools/cycle-summary"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HostToolSigner.TENANT_HEADER, "demo"))
                .andExpect(callbackRequest -> {
                    String body = ((MockClientHttpRequest) callbackRequest).getBodyAsString();
                    String timestamp = callbackRequest.getHeaders().getFirst(HostToolSigner.TIMESTAMP_HEADER);
                    String signature = callbackRequest.getHeaders().getFirst(HostToolSigner.SIGNATURE_HEADER);

                    assertThat(timestamp).isNotBlank();
                    assertThat(callbackRequest.getHeaders().getFirst(HostToolSigner.REQUEST_ID_HEADER))
                            .isEqualTo(request.requestId().toString());
                    assertThat(signature).isEqualTo(signer.sign(timestamp, body, "test-secret"));
                    assertThat(body).contains("\"toolName\":\"cycle-summary\"");
                })
                .andRespond(withSuccess("""
                        {
                          "toolName": "cycle-summary",
                          "status": "OK",
                          "summary": "Cycle summary loaded.",
                          "facts": {"nextPeriodStart": "2026-06-20"},
                          "userExplanation": "Used Flowelle data."
                        }
                        """, MediaType.APPLICATION_JSON));

        HostToolResponse response = client.invoke(toolConfig, request);

        assertThat(response.status()).isEqualTo("OK");
        assertThat(response.facts()).containsEntry("nextPeriodStart", "2026-06-20");
        server.verify();
    }
}
