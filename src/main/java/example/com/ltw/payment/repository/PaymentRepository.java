package example.com.ltw.payment.repository;

import example.com.ltw.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join fetch p.order where p.order.id = :orderId")
    Optional<Payment> findByOrderIdForUpdate(@Param("orderId") Long orderId);

    @Query("select p from Payment p where p.order.id = :orderId")
    Optional<Payment> findByOrderId(@Param("orderId") Long orderId);

    Optional<Payment> findByTransactionNo(String transactionNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join fetch p.order where p.transactionNo = :transactionNo")
    Optional<Payment> findByTransactionNoForUpdate(@Param("transactionNo") String transactionNo);
}
