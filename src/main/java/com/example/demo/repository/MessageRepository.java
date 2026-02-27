package com.example.demo.repository;

import com.example.demo.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :convId ORDER BY m.createdAt DESC")
    List<Message> findTopByConversationIdOrderByCreatedAtDesc(
            @Param("convId") UUID conversationId, Pageable pageable);
}
