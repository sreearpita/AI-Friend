package com.example.demo.security;

import com.example.demo.config.AiFriendProperties;
import com.example.demo.exception.ApiException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EnvSecretResolver implements SecretResolver {
    private static final String ENV_PREFIX = "env://";
    private static final String LITERAL_PREFIX = "literal://";

    private final AiFriendProperties properties;

    public EnvSecretResolver(AiFriendProperties properties) {
        this.properties = properties;
    }

    @Override
    public String resolve(String secretRef) {
        if (!StringUtils.hasText(secretRef)) {
            throw unavailable();
        }
        if (secretRef.startsWith(ENV_PREFIX)) {
            String variableName = secretRef.substring(ENV_PREFIX.length());
            String value = System.getenv(variableName);
            if (!StringUtils.hasText(value)) {
                throw unavailable();
            }
            return value;
        }
        if (secretRef.startsWith(LITERAL_PREFIX) && properties.getSecrets().isAllowLiteral()) {
            return secretRef.substring(LITERAL_PREFIX.length());
        }
        throw unavailable();
    }

    private ApiException unavailable() {
        return new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "SECRET_UNAVAILABLE",
                "Tool signing secret is not available.");
    }
}
