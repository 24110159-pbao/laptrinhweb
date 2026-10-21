package example.com.ltw.order.repository;

import example.com.ltw.order.entity.Order;
import example.com.ltw.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByBranch_IdOrderByCreatedAtDesc(Long branchId);

    Page<Order> findAllByBranch_IdOrderByCreatedAtDesc(Long branchId, Pageable pageable);

    Page<Order> findAllByBranch_IdAndStatusOrderByCreatedAtDesc(Long branchId, OrderStatus status, Pageable pageable);

    Optional<Order> findByIdAndBranch_Id(Long id, Long branchId);

    Optional<Order> findByOrderCode(String orderCode);

    long countByBranch_Id(Long branchId);

    long countByBranch_IdAndStatus(Long branchId, OrderStatus status);

    List<Order> findTop5ByBranch_IdOrderByCreatedAtDesc(Long branchId);

    long countByBranch_IdAndPaymentStatus(Long branchId, example.com.ltw.order.entity.PaymentStatus paymentStatus);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.branch.id = :branchId AND o.status != example.com.ltw.order.entity.OrderStatus.CANCELLED")
    java.math.BigDecimal sumTotalRevenueByBranch(@Param("branchId") Long branchId);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.branch.id = :branchId AND o.paymentStatus = example.com.ltw.order.entity.PaymentStatus.PAID")
    java.math.BigDecimal sumPaidRevenueByBranch(@Param("branchId") Long branchId);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.branch.id = :branchId AND o.paymentStatus = example.com.ltw.order.entity.PaymentStatus.UNPAID AND o.status != example.com.ltw.order.entity.OrderStatus.CANCELLED")
    java.math.BigDecimal sumPendingCodRevenueByBranch(@Param("branchId") Long branchId);

    List<Order> findTop5ByBranch_IdAndStatusOrderByCreatedAtDesc(Long branchId, OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.branch.id = :branchId " +
            "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR o.createdAt <= :toDate)")
    long countByBranchAndTime(
            @Param("branchId") Long branchId,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate
    );

    @Query("SELECT COUNT(o) FROM Order o WHERE o.branch.id = :branchId " +
            "AND o.status = :status " +
            "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR o.createdAt <= :toDate)")
    long countByBranchAndStatusAndTime(
            @Param("branchId") Long branchId,
            @Param("status") OrderStatus status,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate
    );

    @Query("SELECT COUNT(o) FROM Order o WHERE o.branch.id = :branchId " +
            "AND o.paymentStatus = :paymentStatus " +
            "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR o.createdAt <= :toDate)")
    long countByBranchAndPaymentStatusAndTime(
            @Param("branchId") Long branchId,
            @Param("paymentStatus") example.com.ltw.order.entity.PaymentStatus paymentStatus,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate
    );

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.branch.id = :branchId " +
            "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR o.createdAt <= :toDate)")
    java.math.BigDecimal sumTotalOrderAmountByBranchAndTime(
            @Param("branchId") Long branchId,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate
    );

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.branch.id = :branchId " +
            "AND o.status != example.com.ltw.order.entity.OrderStatus.CANCELLED " +
            "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR o.createdAt <= :toDate)")
    java.math.BigDecimal sumTotalRevenueByBranchAndTime(
            @Param("branchId") Long branchId,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate
    );

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.branch.id = :branchId " +
            "AND o.paymentStatus = example.com.ltw.order.entity.PaymentStatus.PAID " +
            "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR o.createdAt <= :toDate)")
    java.math.BigDecimal sumPaidRevenueByBranchAndTime(
            @Param("branchId") Long branchId,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate
    );

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.branch.id = :branchId " +
            "AND o.paymentStatus = example.com.ltw.order.entity.PaymentStatus.UNPAID " +
            "AND o.status != example.com.ltw.order.entity.OrderStatus.CANCELLED " +
            "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR o.createdAt <= :toDate)")
    java.math.BigDecimal sumPendingCodRevenueByBranchAndTime(
            @Param("branchId") Long branchId,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate
    );

    @Query("SELECT COUNT(o) FROM Order o WHERE o.branch.id = :branchId " +
            "AND o.paymentMethod = example.com.ltw.order.entity.PaymentMethod.COD " +
            "AND o.paymentStatus = example.com.ltw.order.entity.PaymentStatus.UNPAID " +
            "AND o.status != example.com.ltw.order.entity.OrderStatus.CANCELLED " +
            "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR o.createdAt <= :toDate)")
    long countPendingCodOrdersByBranchAndTime(
            @Param("branchId") Long branchId,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate
    );

    @Query("SELECT o FROM Order o WHERE o.branch.id = :branchId " +
            "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR o.createdAt <= :toDate) " +
            "ORDER BY o.createdAt DESC")
    List<Order> findRecentOrdersByBranchAndTime(
            @Param("branchId") Long branchId,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate,
            Pageable pageable
    );

    @Query("SELECT o FROM Order o WHERE o.branch.id = :branchId " +
            "AND o.status = :status " +
            "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR o.createdAt <= :toDate) " +
            "ORDER BY o.createdAt DESC")
    List<Order> findUrgentOrdersByBranchAndTime(
            @Param("branchId") Long branchId,
            @Param("status") OrderStatus status,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate,
            Pageable pageable
    );

    @Query("SELECT o FROM Order o WHERE o.branch.id = :branchId " +
            "AND (:status IS NULL OR o.status = :status) " +
            "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR o.createdAt <= :toDate) " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "     LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(o.receiverName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(o.receiverPhone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY o.createdAt DESC")
    Page<Order> searchOrdersByBranchAndTime(
            @Param("branchId") Long branchId,
            @Param("status") OrderStatus status,
            @Param("keyword") String keyword,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate,
            Pageable pageable
    );

    @Query("SELECT o FROM Order o WHERE o.branch.id = :branchId " +
            "AND (:status IS NULL OR o.status = :status) " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "     LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(o.receiverName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(o.receiverPhone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY o.createdAt DESC")
    Page<Order> searchOrdersByBranch(
            @Param("branchId") Long branchId,
            @Param("status") OrderStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
