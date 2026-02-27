package com.example.demo.service;

import com.example.demo.model.AppUser;
import com.example.demo.model.Conversation;
import com.example.demo.model.Message;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConversationServiceTest {

    private ConversationRepository conversationRepository;
    private MessageRepository messageRepository;
    private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        conversationRepository = mock(ConversationRepository.class);
        messageRepository = mock(MessageRepository.class);
        conversationService = new ConversationService(conversationRepository, messageRepository);
    }

    @Test
    void createConversationPersistsAndReturns() {
        AppUser user = new AppUser(UUID.randomUUID());
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Conversation conv = conversationService.create(user);

        assertThat(conv.getId()).isNotNull();
        assertThat(conv.getUser()).isEqualTo(user);
        verify(conversationRepository).save(conv);
    }

    @Test
    void addMessagePersistsMessage() {
        Conversation conv = new Conversation();
        conv.setId(UUID.randomUUID());

        when(messageRepository.save(any(Message.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Message msg = conversationService.addMessage(conv, "user", "Hello");

        assertThat(msg.getId()).isNotNull();
        assertThat(msg.getRole()).isEqualTo("user");
        assertThat(msg.getContent()).isEqualTo("Hello");
        verify(messageRepository).save(msg);
    }

    @Test
    void findByIdDelegatesToRepository() {
        UUID id = UUID.randomUUID();
        Conversation conv = new Conversation();
        conv.setId(id);
        when(conversationRepository.findById(id)).thenReturn(Optional.of(conv));

        Optional<Conversation> result = conversationService.findById(id);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
    }

    @Test
    void getRecentMessagesReturnsChronologicalOrder() {
        UUID convId = UUID.randomUUID();
        Message m1 = new Message();
        m1.setId(UUID.randomUUID());
        m1.setContent("second");
        Message m2 = new Message();
        m2.setId(UUID.randomUUID());
        m2.setContent("first");

        // Repository returns DESC order (newest first)
        when(messageRepository.findTopByConversationIdOrderByCreatedAtDesc(eq(convId), any()))
                .thenReturn(List.of(m1, m2));

        List<Message> result = conversationService.getRecentMessages(convId, 5);

        // Service reverses to chronological (oldest first)
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("first");
        assertThat(result.get(1).getContent()).isEqualTo("second");
    }
}
