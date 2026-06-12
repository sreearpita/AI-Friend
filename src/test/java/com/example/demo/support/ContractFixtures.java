package com.example.demo.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ContractFixtures {
    private ContractFixtures() {
    }

    public static String read(String resourcePath) {
        try (InputStream inputStream = ContractFixtures.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing contract fixture: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read contract fixture: " + resourcePath, exception);
        }
    }
}
