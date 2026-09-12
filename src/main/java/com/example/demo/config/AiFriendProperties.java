package com.example.demo.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aif")
public class AiFriendProperties {
    private final Security security = new Security();
    private final Chat chat = new Chat();
    private final Tools tools = new Tools();
    private final Retrieval retrieval = new Retrieval();
    private final Redis redis = new Redis();
    private final RateLimits rateLimits = new RateLimits();
    private final Admin admin = new Admin();
    private final Secrets secrets = new Secrets();

    public Security getSecurity() {
        return security;
    }

    public Chat getChat() {
        return chat;
    }

    public Tools getTools() {
        return tools;
    }

    public Retrieval getRetrieval() {
        return retrieval;
    }

    public Redis getRedis() {
        return redis;
    }

    public RateLimits getRateLimits() {
        return rateLimits;
    }

    public Admin getAdmin() {
        return admin;
    }

    public Secrets getSecrets() {
        return secrets;
    }

    public static class Security {
        private String apiKeyHeader = "X-AIF-Tenant-Key";
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:3000"));
        private boolean seedDemoTenant = true;
        private String demoTenantSlug = "demo";
        private String demoTenantName = "Demo Tenant";
        private String demoApiKey = "dev-aif-demo-key";

        public String getApiKeyHeader() {
            return apiKeyHeader;
        }

        public void setApiKeyHeader(String apiKeyHeader) {
            this.apiKeyHeader = apiKeyHeader;
        }

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public boolean isSeedDemoTenant() {
            return seedDemoTenant;
        }

        public void setSeedDemoTenant(boolean seedDemoTenant) {
            this.seedDemoTenant = seedDemoTenant;
        }

        public String getDemoTenantSlug() {
            return demoTenantSlug;
        }

        public void setDemoTenantSlug(String demoTenantSlug) {
            this.demoTenantSlug = demoTenantSlug;
        }

        public String getDemoTenantName() {
            return demoTenantName;
        }

        public void setDemoTenantName(String demoTenantName) {
            this.demoTenantName = demoTenantName;
        }

        public String getDemoApiKey() {
            return demoApiKey;
        }

        public void setDemoApiKey(String demoApiKey) {
            this.demoApiKey = demoApiKey;
        }
    }

    public static class Chat {
        private int maxHistoryMessages = 8;
        private int maxMessageLength = 4000;

        public int getMaxHistoryMessages() {
            return maxHistoryMessages;
        }

        public void setMaxHistoryMessages(int maxHistoryMessages) {
            this.maxHistoryMessages = maxHistoryMessages;
        }

        public int getMaxMessageLength() {
            return maxMessageLength;
        }

        public void setMaxMessageLength(int maxMessageLength) {
            this.maxMessageLength = maxMessageLength;
        }
    }

    public static class Tools {
        private boolean seedDemoTools = false;
        private String demoCallbackUrl = "http://localhost:8090/aif/tools";
        private String demoSigningSecret = "dev-aif-tool-secret";
        private String demoSigningSecretRef = "env://AIF_DEMO_TOOL_SIGNING_SECRET";
        private String demoSigningKeyId = "dev-v1";
        private int requestTimeoutMs = 2000;

        public boolean isSeedDemoTools() {
            return seedDemoTools;
        }

        public void setSeedDemoTools(boolean seedDemoTools) {
            this.seedDemoTools = seedDemoTools;
        }

        public String getDemoCallbackUrl() {
            return demoCallbackUrl;
        }

        public void setDemoCallbackUrl(String demoCallbackUrl) {
            this.demoCallbackUrl = demoCallbackUrl;
        }

        public String getDemoSigningSecret() {
            return demoSigningSecret;
        }

        public void setDemoSigningSecret(String demoSigningSecret) {
            this.demoSigningSecret = demoSigningSecret;
        }

        public String getDemoSigningSecretRef() {
            return demoSigningSecretRef;
        }

        public void setDemoSigningSecretRef(String demoSigningSecretRef) {
            this.demoSigningSecretRef = demoSigningSecretRef;
        }

        public String getDemoSigningKeyId() {
            return demoSigningKeyId;
        }

        public void setDemoSigningKeyId(String demoSigningKeyId) {
            this.demoSigningKeyId = demoSigningKeyId;
        }

        public int getRequestTimeoutMs() {
            return requestTimeoutMs;
        }

        public void setRequestTimeoutMs(int requestTimeoutMs) {
            this.requestTimeoutMs = requestTimeoutMs;
        }
    }

    public static class Retrieval {
        private boolean enabled = true;
        private int maxCitations = 3;
        private int minQueryLength = 4;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxCitations() {
            return maxCitations;
        }

        public void setMaxCitations(int maxCitations) {
            this.maxCitations = maxCitations;
        }

        public int getMinQueryLength() {
            return minQueryLength;
        }

        public void setMinQueryLength(int minQueryLength) {
            this.minQueryLength = minQueryLength;
        }
    }

    public static class Redis {
        private boolean enabled = false;
        private String host = "localhost";
        private int port = 6379;
        private String password = "";
        private int timeoutMs = 1000;
        private String keyPrefix = "aif";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
    }

    public static class RateLimits {
        private boolean enabled = true;
        private int userPerMinute = 20;
        private int tenantPerMinute = 120;
        private int tenantPerDay = 10000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getUserPerMinute() {
            return userPerMinute;
        }

        public void setUserPerMinute(int userPerMinute) {
            this.userPerMinute = userPerMinute;
        }

        public int getTenantPerMinute() {
            return tenantPerMinute;
        }

        public void setTenantPerMinute(int tenantPerMinute) {
            this.tenantPerMinute = tenantPerMinute;
        }

        public int getTenantPerDay() {
            return tenantPerDay;
        }

        public void setTenantPerDay(int tenantPerDay) {
            this.tenantPerDay = tenantPerDay;
        }
    }

    public static class Admin {
        private String issuer = "";
        private String audience = "ai-friend-admin";
        private String jwksUri = "";
        private String requiredRole = "aif-admin";
        private int maxTokenLifetimeSeconds = 300;

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }

        public String getJwksUri() {
            return jwksUri;
        }

        public void setJwksUri(String jwksUri) {
            this.jwksUri = jwksUri;
        }

        public String getRequiredRole() {
            return requiredRole;
        }

        public void setRequiredRole(String requiredRole) {
            this.requiredRole = requiredRole;
        }

        public int getMaxTokenLifetimeSeconds() {
            return maxTokenLifetimeSeconds;
        }

        public void setMaxTokenLifetimeSeconds(int maxTokenLifetimeSeconds) {
            this.maxTokenLifetimeSeconds = maxTokenLifetimeSeconds;
        }
    }

    public static class Secrets {
        private boolean allowLiteral = false;

        public boolean isAllowLiteral() {
            return allowLiteral;
        }

        public void setAllowLiteral(boolean allowLiteral) {
            this.allowLiteral = allowLiteral;
        }
    }
}
