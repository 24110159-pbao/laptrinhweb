package example.com.ltw.discount.repository;

import example.com.ltw.discount.entity.DiscountUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscountUsageRepository extends JpaRepository<DiscountUsage, Long> {
    long countByDiscountCodeIdAndUserId(Long discountCodeId, Long userId);
    boolean existsByOrderId(Long orderId);
    void deleteByOrderId(Long orderId);
}
