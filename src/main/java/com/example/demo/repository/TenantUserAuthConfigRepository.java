package com.example.demo.repository;

import java.util.Optional;
import java.util.UUID;

import com.example.demo.model.TenantUserAuthConfig;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantUserAuthConfigRepository extends JpaRepository<TenantUserAuthConfig, UUID> {
    Optional<TenantUserAuthConfig> findByTenantIdAndActiveTrue(UUID tenantId);
}
