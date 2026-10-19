package example.com.ltw.order.repository;

import example.com.ltw.order.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    List<OrderStatusHistory> findAllByOrderIdOrderByCreatedAtAsc(Long orderId);
}
