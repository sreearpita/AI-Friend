package com.example.demo.repository;

import java.util.Optional;
import java.util.UUID;

import com.example.demo.model.ChatSession;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    Optional<ChatSession> findByIdAndTenantId(UUID id, UUID tenantId);
}
