package example.com.ltw.chat.repository;

import example.com.ltw.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @EntityGraph(attributePaths = {"sender"})
    List<ChatMessage> findAllByConversationIdOrderByCreatedAtAsc(Long conversationId);

    long countByConversationIdAndSenderIdNotAndReadAtIsNull(Long conversationId, Long senderId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.readAt = :now WHERE m.conversation.id = :conversationId AND m.sender.id != :readerId AND m.readAt IS NULL")
    int markAllAsRead(@Param("conversationId") Long conversationId, @Param("readerId") Long readerId, @Param("now") LocalDateTime now);
}
