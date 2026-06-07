package com.example.demo.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.example.demo.model.AuditEvent;
import com.example.demo.model.Tenant;
import com.example.demo.repository.AuditEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditEventRepository auditEventRepository, ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
    }

    public void record(Tenant tenant, String externalUserId, UUID sessionId, String eventType, Map<String, ?> metadata) {
        auditEventRepository.save(new AuditEvent(
                tenant.getId(),
                externalUserId,
                sessionId,
                eventType,
                toJson(metadata)));
    }

    public Map<String, Object> metadata() {
        return new LinkedHashMap<>();
    }

    private String toJson(Map<String, ?> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            logger.warn("Audit metadata serialization failed: {}", exception.getMessage());
            return "{}";
        }
    }
}
