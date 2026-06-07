package com.example.demo.repository;

import java.util.UUID;

import com.example.demo.model.AuditEvent;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
}
