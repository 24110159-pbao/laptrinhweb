package example.com.ltw.checkout.repository;

import example.com.ltw.checkout.entity.CheckoutSession;
import example.com.ltw.checkout.entity.CheckoutStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CheckoutSessionRepository extends JpaRepository<CheckoutSession, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CheckoutSession c join fetch c.cart where c.id = :id and c.user.id = :userId")
    Optional<CheckoutSession> findForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    Optional<CheckoutSession> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CheckoutSession c where c.id = :id")
    Optional<CheckoutSession> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CheckoutSession c join fetch c.cart where c.orderCode = :orderCode")
    Optional<CheckoutSession> findByOrderCodeForUpdate(@Param("orderCode") String orderCode);

    boolean existsByCartIdAndStatus(Long cartId, CheckoutStatus status);

    Optional<CheckoutSession> findFirstByCartIdAndStatusOrderByIdDesc(Long cartId, CheckoutStatus status);

    List<CheckoutSession> findByStatusAndExpiresAtLessThanEqualOrderById(CheckoutStatus status, LocalDateTime now);
}
