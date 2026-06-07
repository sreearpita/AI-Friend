package com.example.demo.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HostToolSignerTest {
    @Test
    void signsTimestampAndBodyWithHmacSha256() {
        HostToolSigner signer = new HostToolSigner();

        String signature = signer.sign("2026-06-07T00:00:00Z", "{\"toolName\":\"cycle-summary\"}", "secret");

        assertThat(signature)
                .isEqualTo("61d8139ea063c314927f82377accf25429264e2bccd97503d066ae9d43b2edb2");
    }
}
