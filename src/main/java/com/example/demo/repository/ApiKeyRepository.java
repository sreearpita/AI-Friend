package com.example.demo.repository;

import java.util.Optional;
import java.util.UUID;

import com.example.demo.model.ApiKey;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByKeyHashAndActiveTrue(String keyHash);

    @Query("select apiKey from ApiKey apiKey join fetch apiKey.tenant where apiKey.keyHash = :keyHash and apiKey.active = true")
    Optional<ApiKey> findActiveByKeyHashWithTenant(@Param("keyHash") String keyHash);

    boolean existsByKeyHash(String keyHash);
}
