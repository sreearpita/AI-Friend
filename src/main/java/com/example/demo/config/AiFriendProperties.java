package com.example.demo.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aif")
public class AiFriendProperties {
    private final Security security = new Security();
    private final Chat chat = new Chat();

    public Security getSecurity() {
        return security;
    }

    public Chat getChat() {
        return chat;
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
}
