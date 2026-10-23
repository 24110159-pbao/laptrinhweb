package example.com.ltw.order.repository;

import example.com.ltw.order.entity.OrderItem;

import example.com.ltw.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findAllByOrder_IdOrderByIdAsc(Long orderId);

    @Query("""
            select oi.productVariant.product.id as productId, sum(oi.quantity) as quantitySold
            from OrderItem oi
            where oi.order.status = :status
            group by oi.productVariant.product.id
            """)
    List<OrderItemCatalogProjection> findSalesByProduct(@Param("status") OrderStatus status);

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
