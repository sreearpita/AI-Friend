package com.example.demo.repository;

import java.util.Optional;
import java.util.UUID;

import com.example.demo.model.Tenant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySlug(String slug);
}
