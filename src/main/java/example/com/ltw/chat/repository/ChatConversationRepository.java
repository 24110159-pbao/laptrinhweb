package example.com.ltw.chat.repository;

import example.com.ltw.chat.entity.ChatConversation;
import example.com.ltw.chat.entity.ChatConversationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    @EntityGraph(attributePaths = {"user", "staff", "branch"})
    Optional<ChatConversation> findFirstByUserIdAndBranchIdAndStatus(Long userId, Long branchId, ChatConversationStatus status);

    @EntityGraph(attributePaths = {"user", "staff", "branch"})
    Optional<ChatConversation> findFirstByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, ChatConversationStatus status);

    @EntityGraph(attributePaths = {"user", "staff", "branch"})
    List<ChatConversation> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "staff", "branch"})
    List<ChatConversation> findAllByBranchIdOrderByUpdatedAtDesc(Long branchId);

    @EntityGraph(attributePaths = {"user", "staff", "branch"})
    List<ChatConversation> findAllByBranchIdAndStatusOrderByUpdatedAtDesc(Long branchId, ChatConversationStatus status);

    @Query("SELECT COUNT(c) FROM ChatConversation c WHERE c.branch.id = :branchId AND c.status = :status")
    long countByBranchIdAndStatus(@Param("branchId") Long branchId, @Param("status") ChatConversationStatus status);
}
