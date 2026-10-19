package example.com.ltw.order.repository;

import example.com.ltw.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    @EntityGraph(attributePaths = {"productVariant", "order"})
    List<OrderItem> findByOrderIdOrderById(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from OrderItem i join fetch i.order where i.id = :id and i.order.id = :orderId")
    Optional<OrderItem> findByIdAndOrderIdForUpdate(@Param("id") Long id, @Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from OrderItem i where i.id = :itemId")
    Optional<OrderItem> findForReview(@Param("itemId") Long itemId);

    void deleteByOrderId(Long orderId);
}
