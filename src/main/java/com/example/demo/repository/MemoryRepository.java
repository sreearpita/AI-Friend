package com.example.demo.repository;

import com.example.demo.model.Memory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoryRepository extends JpaRepository<Memory, UUID> {

    @Query("SELECT m FROM Memory m WHERE m.user.id = :userId ORDER BY m.updatedAt DESC")
    List<Memory> findTopByUserIdOrderByUpdatedAtDesc(
            @Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT m FROM Memory m WHERE m.user.id = :userId AND LOWER(m.fact) = LOWER(:fact)")
    Optional<Memory> findByUserIdAndFactIgnoreCase(
            @Param("userId") UUID userId, @Param("fact") String fact);
}
