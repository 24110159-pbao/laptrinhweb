package example.com.ltw.order.repository;

import example.com.ltw.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    Optional<Order> findByOrderCode(String orderCode);

    Optional<Order> findByOrderCodeAndUserId(String orderCode, Long userId);

    @EntityGraph(attributePaths = {"branch"})
    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"branch"})
    Optional<Order> findWithBranchByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"branch"})
    Optional<Order> findWithBranchByOrderCodeAndUserId(String orderCode, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.orderCode = :orderCode")
    Optional<Order> findByOrderCodeForUpdate(@Param("orderCode") String orderCode);

    List<Order> findByPaymentStatusAndPaymentMethod(
            example.com.ltw.order.entity.PaymentStatus paymentStatus,
            example.com.ltw.order.entity.PaymentMethod paymentMethod
    );

    List<Order> findByPaymentStatusAndPaymentMethodAndPaymentExpiresAtLessThanEqual(
            example.com.ltw.order.entity.PaymentStatus paymentStatus,
            example.com.ltw.order.entity.PaymentMethod paymentMethod,
            LocalDateTime now
    );
}
