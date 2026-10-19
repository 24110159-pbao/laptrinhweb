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
}
