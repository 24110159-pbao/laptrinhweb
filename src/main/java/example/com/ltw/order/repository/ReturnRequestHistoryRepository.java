package example.com.ltw.order.repository;

import example.com.ltw.order.entity.ReturnRequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnRequestHistoryRepository extends JpaRepository<ReturnRequestHistory, Long> {
    List<ReturnRequestHistory> findAllByReturnRequestIdOrderByCreatedAtAsc(Long returnRequestId);
}
