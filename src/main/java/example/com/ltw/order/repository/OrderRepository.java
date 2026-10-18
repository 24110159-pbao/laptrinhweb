package example.com.ltw.order.repository;

import example.com.ltw.order.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "user")
    List<Order> findAllByBranch_IdOrderByCreatedAtDesc(Long branchId);

    @EntityGraph(attributePaths = "user")
    Optional<Order> findByIdAndBranch_Id(Long id, Long branchId);

    long countByBranch_Id(Long branchId);
}
