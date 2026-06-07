package com.example.demo.repository;

import java.util.Optional;
import java.util.UUID;

import com.example.demo.model.TenantToolConfig;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantToolConfigRepository extends JpaRepository<TenantToolConfig, UUID> {
    Optional<TenantToolConfig> findByTenantIdAndNameAndActiveTrue(UUID tenantId, String name);

    boolean existsByTenantIdAndName(UUID tenantId, String name);
}
