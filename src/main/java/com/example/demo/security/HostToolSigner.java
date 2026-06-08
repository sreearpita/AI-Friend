package com.example.demo.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class HostToolSigner {
    public static final String TENANT_HEADER = "X-AIF-Tenant";
    public static final String TIMESTAMP_HEADER = "X-AIF-Timestamp";
    public static final String SIGNATURE_HEADER = "X-AIF-Signature";
    public static final String REQUEST_ID_HEADER = "X-AIF-Request-Id";
    public static final String KEY_ID_HEADER = "X-AIF-Key-Id";

    public String sign(String timestamp, String body, String signingSecret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Unable to sign host tool request", exception);
        }
    }
}
