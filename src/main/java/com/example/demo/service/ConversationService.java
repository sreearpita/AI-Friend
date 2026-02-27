package com.example.demo.service;

import com.example.demo.model.AppUser;
import com.example.demo.model.Conversation;
import com.example.demo.model.Message;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.MessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public Conversation create(AppUser user) {
        Conversation conv = new Conversation();
        conv.setId(UUID.randomUUID());
        conv.setUser(user);
        return conversationRepository.save(conv);
    }

    public Optional<Conversation> findById(UUID id) {
        return conversationRepository.findById(id);
    }

    @Transactional
    public Message addMessage(Conversation conversation, String role, String content) {
        Message msg = new Message();
        msg.setId(UUID.randomUUID());
        msg.setConversation(conversation);
        msg.setRole(role);
        msg.setContent(content);
        return messageRepository.save(msg);
    }

    /**
     * Returns up to {@code limit} most-recent messages for a conversation,
     * in chronological order (oldest first).
     */
    public List<Message> getRecentMessages(UUID conversationId, int limit) {
        List<Message> desc = messageRepository
                .findTopByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(0, limit));
        List<Message> mutable = new java.util.ArrayList<>(desc);
        Collections.reverse(mutable);
        return mutable;
    }
}
