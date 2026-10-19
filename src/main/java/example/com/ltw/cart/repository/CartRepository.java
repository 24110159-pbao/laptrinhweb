package example.com.ltw.cart.repository;

import example.com.ltw.cart.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = {
            "branch",
            "items",
            "items.inventory",
            "items.inventory.variant",
            "items.inventory.variant.product"
    })
    List<Cart> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<Cart> findByUserIdAndBranchId(Long userId, Long branchId);

    Optional<Cart> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "branch", "items", "items.inventory", "items.inventory.variant", "items.inventory.variant.product"
    })
    @Query("select c from Cart c where c.id = :cartId and c.user.id = :userId")
    Optional<Cart> findByIdForUpdateAndUserId(@Param("cartId") Long cartId, @Param("userId") Long userId);
}
