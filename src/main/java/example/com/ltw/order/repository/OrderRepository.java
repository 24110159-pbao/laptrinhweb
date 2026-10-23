package example.com.ltw.order.repository;

import example.com.ltw.order.entity.Order;
import example.com.ltw.order.entity.OrderStatus;
import example.com.ltw.order.entity.PaymentMethod;
import example.com.ltw.order.entity.PaymentStatus;
import example.com.ltw.user.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
            "AND o.status != example.com.ltw.order.entity.OrderStatus.PENDING " +
            "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR o.createdAt <= :toDate) " +
            "ORDER BY CASE WHEN o.status = example.com.ltw.order.entity.OrderStatus.CONFIRMED THEN 0 " +
            "              WHEN o.status = example.com.ltw.order.entity.OrderStatus.SHIPPING THEN 1 " +
            "              ELSE 2 END, o.updatedAt DESC, o.createdAt DESC")
    List<Order> findProcessingOrdersByBranchAndTime(
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
            "AND o.status = example.com.ltw.order.entity.OrderStatus.CONFIRMED " +
            "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR o.createdAt <= :toDate) " +
            "ORDER BY o.createdAt ASC")
    List<Order> findUrgentConfirmedOrdersByBranchAndTime(
            @Param("branchId") Long branchId,
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

    @Query("SELECT o FROM Order o WHERE o.branch.id = :branchId " +
            "AND (o.assignedStaff.id = :staffId OR (o.assignedStaff IS NULL AND (" +
            "     LOWER(COALESCE(o.note, '')) LIKE LOWER(CONCAT('%', :staffEmail, '%')) OR " +
            "     (:staffName <> '' AND LOWER(COALESCE(o.note, '')) LIKE LOWER(CONCAT('%', :staffName, '%')))" +
            "))) " +
            "AND (:status IS NULL OR o.status = :status) " +
            "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR o.createdAt <= :toDate) " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "     LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(o.receiverName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(o.receiverPhone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY o.createdAt DESC")
    Page<Order> searchMyManagedOrders(
            @Param("branchId") Long branchId,
            @Param("staffId") Long staffId,
            @Param("staffEmail") String staffEmail,
            @Param("staffName") String staffName,
            @Param("status") OrderStatus status,
            @Param("keyword") String keyword,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate,
            Pageable pageable
    );

    @Query("SELECT COUNT(o) FROM Order o WHERE o.branch.id = :branchId " +
            "AND (o.assignedStaff.id = :staffId OR (o.assignedStaff IS NULL AND (" +
            "     LOWER(COALESCE(o.note, '')) LIKE LOWER(CONCAT('%', :staffEmail, '%')) OR " +
            "     (:staffName <> '' AND LOWER(COALESCE(o.note, '')) LIKE LOWER(CONCAT('%', :staffName, '%')))" +
            ")))")
    long countMyManagedOrders(
            @Param("branchId") Long branchId,
            @Param("staffId") Long staffId,
            @Param("staffEmail") String staffEmail,
            @Param("staffName") String staffName
    );

    @Query("SELECT COUNT(o) FROM Order o WHERE o.branch.id = :branchId " +
            "AND (o.assignedStaff.id = :staffId OR (o.assignedStaff IS NULL AND (" +
            "     LOWER(COALESCE(o.note, '')) LIKE LOWER(CONCAT('%', :staffEmail, '%')) OR " +
            "     (:staffName <> '' AND LOWER(COALESCE(o.note, '')) LIKE LOWER(CONCAT('%', :staffName, '%')))" +
            "))) " +
            "AND (:status IS NULL OR o.status = :status) " +
            "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR o.createdAt <= :toDate)")
    long countMyManagedOrdersByStatusAndTime(
            @Param("branchId") Long branchId,
            @Param("staffId") Long staffId,
            @Param("staffEmail") String staffEmail,
            @Param("staffName") String staffName,
            @Param("status") OrderStatus status,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate
    );

    @Query("SELECT COUNT(o) FROM Order o WHERE o.branch.id = :branchId " +
            "AND (o.assignedStaff.id = :staffId OR (o.assignedStaff IS NULL AND (" +
            "     LOWER(COALESCE(o.note, '')) LIKE LOWER(CONCAT('%', :staffEmail, '%')) OR " +
            "     (:staffName <> '' AND LOWER(COALESCE(o.note, '')) LIKE LOWER(CONCAT('%', :staffName, '%')))" +
            "))) " +
            "AND o.paymentStatus = :paymentStatus " +
            "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR o.createdAt <= :toDate)")
    long countMyManagedOrdersByPaymentStatusAndTime(
            @Param("branchId") Long branchId,
            @Param("staffId") Long staffId,
            @Param("staffEmail") String staffEmail,
            @Param("staffName") String staffName,
            @Param("paymentStatus") example.com.ltw.order.entity.PaymentStatus paymentStatus,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate
    );

    @Query("SELECT o FROM Order o WHERE o.branch.id = :branchId " +
            "AND (o.assignedStaff.id = :staffId OR (o.assignedStaff IS NULL AND (" +
            "     LOWER(COALESCE(o.note, '')) LIKE LOWER(CONCAT('%', :staffEmail, '%')) OR " +
            "     (:staffName <> '' AND LOWER(COALESCE(o.note, '')) LIKE LOWER(CONCAT('%', :staffName, '%')))" +
            "))) " +
            "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR o.createdAt <= :toDate) " +
            "ORDER BY CASE WHEN o.status = example.com.ltw.order.entity.OrderStatus.CONFIRMED THEN 0 " +
            "              WHEN o.status = example.com.ltw.order.entity.OrderStatus.SHIPPING THEN 1 " +
            "              WHEN o.status = example.com.ltw.order.entity.OrderStatus.DELIVERED THEN 2 " +
            "              ELSE 3 END, o.createdAt DESC")
    List<Order> findMyManagedRecentOrders(
            @Param("branchId") Long branchId,
            @Param("staffId") Long staffId,
            @Param("staffEmail") String staffEmail,
            @Param("staffName") String staffName,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate,
            Pageable pageable
    );

    @Query("SELECT o FROM Order o WHERE o.branch.id = :branchId " +
            "AND o.status = example.com.ltw.order.entity.OrderStatus.PENDING " +
            "AND (:fromDate IS NULL OR o.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR o.createdAt <= :toDate) " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "     LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(o.receiverName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(o.receiverPhone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY o.createdAt ASC")
    Page<Order> searchPendingPoolOrders(
            @Param("branchId") Long branchId,
            @Param("keyword") String keyword,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate,
            Pageable pageable
    );


    @EntityGraph(attributePaths = "user")
    List<Order> findAllByBranch_IdOrderByCreatedAtDesc(Long branchId);

    @EntityGraph(attributePaths = "user")
    Optional<Order> findByIdAndBranch_Id(Long id, Long branchId);

    long countByBranch_Id(Long branchId);

    // Danh sách khách hàng đã có đơn tại chi nhánh
    @Query("""
    select distinct u
    from User u
    where u.id in (
        select o.user.id
        from Order o
        where o.branch.id = :branchId
          and o.user is not null
    )
    order by u.fullName
    """)
    List<User> findCustomersByBranchId(
            @Param("branchId") Long branchId
    );
    // Thống kê số đơn theo trạng thái của chi nhánh
    @Query("""
        select o.status, count(o.id)
        from Order o
        where o.branch.id = :branchId
        group by o.status
    """)
    List<Object[]> countOrdersByStatus(
            @Param("branchId") Long branchId
    );

    // Tổng doanh thu của các đơn đã giao thuộc chi nhánh
    @Query("""
        select coalesce(sum(o.finalAmount), 0)
        from Order o
        where o.branch.id = :branchId
          and o.status = example.com.ltw.order.entity.OrderStatus.DELIVERED
    """)
    BigDecimal sumDeliveredRevenueByBranchId(
            @Param("branchId") Long branchId
    );

    // Doanh thu theo tháng trong khoảng thời gian được chọn
    @Query("""
        select
            function('year', o.createdAt),
            function('month', o.createdAt),
            coalesce(sum(o.finalAmount), 0)
        from Order o
        where o.branch.id = :branchId
          and o.status = example.com.ltw.order.entity.OrderStatus.DELIVERED
          and o.createdAt >= :fromDate
          and o.createdAt < :toDate
        group by
            function('year', o.createdAt),
            function('month', o.createdAt)
        order by
            function('year', o.createdAt),
            function('month', o.createdAt)
    """)
    List<Object[]> findMonthlyDeliveredRevenue(
            @Param("branchId") Long branchId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    // đếm số đơn hàng trong tháng
    @Query("""
    SELECT YEAR(o.createdAt), MONTH(o.createdAt), COUNT(o.id)
    FROM Order o
    WHERE o.branch.id = :branchId
      AND o.createdAt >= :fromDate
      AND o.createdAt < :toDate
    GROUP BY YEAR(o.createdAt), MONTH(o.createdAt)
    ORDER BY YEAR(o.createdAt), MONTH(o.createdAt)
""")
    List<Object[]> findMonthlyOrderCounts(
            @Param("branchId") Long branchId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    // ==========================================
    // ADMIN QUERIES
    // ==========================================

    @Query("""
        select coalesce(sum(o.finalAmount), 0)
        from Order o
        where o.status = example.com.ltw.order.entity.OrderStatus.DELIVERED
    """)
    BigDecimal sumAllDeliveredRevenue();

    @Query("""
        select o.branch.id, o.branch.name, coalesce(sum(o.finalAmount), 0), count(o.id)
        from Order o
        where o.status = example.com.ltw.order.entity.OrderStatus.DELIVERED
        group by o.branch.id, o.branch.name
        order by coalesce(sum(o.finalAmount), 0) desc
    """)
    List<Object[]> findRevenueByBranch();

    @Query("""
        select
            function('year', o.createdAt),
            function('month', o.createdAt),
            coalesce(sum(o.finalAmount), 0)
        from Order o
        where o.status = example.com.ltw.order.entity.OrderStatus.DELIVERED
          and o.createdAt >= :fromDate
          and o.createdAt < :toDate
        group by
            function('year', o.createdAt),
            function('month', o.createdAt)
        order by
            function('year', o.createdAt),
            function('month', o.createdAt)
    """)
    List<Object[]> findAllMonthlyDeliveredRevenue(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query("""
        select
            function('year', o.createdAt),
            function('month', o.createdAt),
            count(o.id)
        from Order o
        where o.createdAt >= :fromDate
          and o.createdAt < :toDate
        group by
            function('year', o.createdAt),
            function('month', o.createdAt)
        order by
            function('year', o.createdAt),
            function('month', o.createdAt)
    """)
    List<Object[]> findAllMonthlyOrderCounts(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query("""
        select o.status, count(o.id)
        from Order o
        group by o.status
    """)
    List<Object[]> countAllOrdersByStatus();
}
