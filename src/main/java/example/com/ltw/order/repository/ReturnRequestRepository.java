package example.com.ltw.order.repository;

import example.com.ltw.order.entity.ReturnRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    @EntityGraph(attributePaths = {"items", "items.orderItem"})
    List<ReturnRequest> findAllByOrderIdOrderByCreatedAtDesc(Long orderId);

    @EntityGraph(attributePaths = {"items", "items.orderItem", "order", "user"})
    List<ReturnRequest> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"items", "items.orderItem", "order", "user"})
    List<ReturnRequest> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReturnRequest r where r.id = :id")
    Optional<ReturnRequest> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"items", "items.orderItem", "order", "user"})
    Optional<ReturnRequest> findWithItemsById(Long id);
}
