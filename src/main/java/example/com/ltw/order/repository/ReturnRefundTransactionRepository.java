package example.com.ltw.order.repository;

import example.com.ltw.order.entity.ReturnRefundTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReturnRefundTransactionRepository extends JpaRepository<ReturnRefundTransaction, Long> {
    Optional<ReturnRefundTransaction> findByIdempotencyKey(String idempotencyKey);
}
