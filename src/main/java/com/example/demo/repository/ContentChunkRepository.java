package com.example.demo.repository;

import java.util.List;
import java.util.UUID;

import com.example.demo.model.ContentChunk;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ContentChunkRepository extends JpaRepository<ContentChunk, UUID> {
    @Query("""
            select chunk
            from ContentChunk chunk
            join fetch chunk.source source
            where chunk.active = true
              and chunk.reviewStatus = 'APPROVED'
              and source.active = true
              and source.reviewStatus = 'APPROVED'
            """)
    List<ContentChunk> findApprovedActiveChunksWithSource();
}
