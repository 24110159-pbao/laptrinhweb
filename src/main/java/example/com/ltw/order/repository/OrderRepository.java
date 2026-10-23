package example.com.ltw.order.repository;

import example.com.ltw.order.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import example.com.ltw.user.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

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

}
