package com.example.demo.security;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class ApiKeyGenerator {
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        return "aif_live_" + Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    public String prefix(String key) {
        return key.length() <= 16 ? key : key.substring(0, 16);
    }
}
