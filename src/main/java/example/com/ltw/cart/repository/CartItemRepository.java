package example.com.ltw.cart.repository;

import example.com.ltw.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByIdAndCartUserId(Long id, Long userId);

    Optional<CartItem> findByCartIdAndInventoryId(Long cartId, Long inventoryId);

    void deleteByCartId(Long cartId);
}
