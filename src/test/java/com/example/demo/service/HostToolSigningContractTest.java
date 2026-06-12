package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.example.demo.config.AiFriendProperties;
import com.example.demo.dto.HostToolRequest;
import com.example.demo.model.Tenant;
import com.example.demo.model.TenantToolConfig;
import com.example.demo.security.HostToolSigner;
import com.example.demo.support.ContractFixtures;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class HostToolSigningContractTest {
    @Test
    void signsKnownFixtureBodyWithSharedHmacVector() {
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
                "https://flowelle.example/api/aif/tools/cycle-summary",
                "secret",
                "dev-v1",
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
                Map.of("toolName", "cycle-summary"));

        server.expect(requestTo("https://flowelle.example/api/aif/tools/cycle-summary"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(callbackRequest -> {
                    String body = ((MockClientHttpRequest) callbackRequest).getBodyAsString();
                    String timestamp = "2026-06-07T00:00:00Z";
                    String signingBody = ContractFixtures.read("/contracts/flowelle/signing-body.json").trim();
                    assertThat(body).contains("\"toolName\":\"cycle-summary\"");
                    assertThat(signer.sign(timestamp, signingBody, "secret"))
                            .isEqualTo("61d8139ea063c314927f82377accf25429264e2bccd97503d066ae9d43b2edb2");
                })
                .andRespond(withSuccess(
                        ContractFixtures.read("/contracts/flowelle/cycle-summary-response.json"),
                        MediaType.APPLICATION_JSON));

        client.invoke(toolConfig, request);
        server.verify();
    }
}
