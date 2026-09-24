package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.TenantCapability;

public interface TenantCapabilityRepository extends JpaRepository<TenantCapability, UUID> {
    Optional<TenantCapability> findByTenantIdAndCapabilityKey(UUID tenantId, String capabilityKey);
    List<TenantCapability> findByTenantIdAndActiveTrueOrderByPriorityAscCapabilityKeyAsc(UUID tenantId);
}
