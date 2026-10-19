package example.com.ltw.cart.service;

import example.com.ltw.cart.dto.CartLineView;
import example.com.ltw.cart.dto.CartOption;
import example.com.ltw.cart.dto.CartView;
import example.com.ltw.cart.entity.Cart;
import example.com.ltw.cart.entity.CartItem;
import example.com.ltw.cart.repository.CartItemRepository;
import example.com.ltw.cart.repository.CartRepository;
import example.com.ltw.checkout.entity.CheckoutStatus;
import example.com.ltw.checkout.repository.CheckoutSessionRepository;
import example.com.ltw.branch.entity.BranchStatus;
import example.com.ltw.catalog.entity.ProductStatus;
import example.com.ltw.catalog.entity.ProductVariantStatus;
import example.com.ltw.inventory.entity.BranchInventory;
import example.com.ltw.inventory.repository.BranchInventoryRepository;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BranchInventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final CheckoutSessionRepository checkoutRepository;

    @Transactional(readOnly = true)
    public List<CartView> getCarts(Long userId) {
        return cartRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toCartView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CartOption> getCartOptions() {
        return inventoryRepository.findAllAvailableForCart().stream()
                .filter(inventory -> inventory.getAvailableQuantity() > 0)
                .map(inventory -> new CartOption(
                        inventory.getId(),
                        inventory.getBranch().getName() + " — "
                                + inventory.getVariant().getProduct().getName() + " "
                                + inventory.getVariant().getRam() + " "
                                + inventory.getVariant().getStorage() + " "
                                + inventory.getVariant().getColor()
                                + " (còn " + inventory.getAvailableQuantity() + ")",
                        inventory.getAvailableQuantity()
                ))
                .toList();
    }

    @Transactional
    public void addItem(Long userId, Long inventoryId, int quantity) {
        validatePositiveQuantity(quantity);
        User user = lockUser(userId);
        BranchInventory inventory = lockInventory(inventoryId);
        validatePurchasable(inventory);

        Cart cart = cartRepository.findByUserIdAndBranchId(userId, inventory.getBranch().getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    newCart.setBranch(inventory.getBranch());
                    return cartRepository.save(newCart);
                });
        ensureCartCanChange(cart.getId());

        CartItem item = cartItemRepository
                .findByCartIdAndInventoryId(cart.getId(), inventory.getId())
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(cart);
                    newItem.setInventory(inventory);
                    newItem.setQuantity(0);
                    return newItem;
                });

        long requested = (long) item.getQuantity() + quantity;
        validateAvailableQuantity(requested, inventory);
        item.setQuantity(Math.toIntExact(requested));
        cartItemRepository.save(item);
    }

    @Transactional
    public void updateQuantity(Long userId, Long itemId, int quantity) {
        validatePositiveQuantity(quantity);
        CartItem existing = cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong giỏ hàng."));
        lockUser(userId);
        ensureCartCanChange(existing.getCart().getId());
        BranchInventory inventory = lockInventory(existing.getInventory().getId());
        validatePurchasable(inventory);
        validateAvailableQuantity(quantity, inventory);
        existing.setQuantity(quantity);
    }

    @Transactional
    public void removeItem(Long userId, Long itemId) {
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong giỏ hàng."));
        lockUser(userId);
        ensureCartCanChange(item.getCart().getId());
        cartItemRepository.delete(item);
    }

    private void ensureCartCanChange(Long cartId) {
        if (cartId != null && checkoutRepository.existsByCartIdAndStatus(cartId, CheckoutStatus.ACTIVE)) {
            throw new IllegalStateException("Giỏ hàng đang được Checkout và đã giữ tồn kho; hãy hoàn tất hoặc đợi phiên hết hạn.");
        }
    }

    private User lockUser(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản khách hàng."));
    }

    private BranchInventory lockInventory(Long inventoryId) {
        return inventoryRepository.findByIdForUpdate(inventoryId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không còn tồn tại tại chi nhánh này."));
    }

    private void validatePurchasable(BranchInventory inventory) {
        if (inventory.getBranch().getStatus() != BranchStatus.ACTIVE
                || inventory.getVariant().getStatus() != ProductVariantStatus.ACTIVE
                || inventory.getVariant().getProduct().getStatus() != ProductStatus.ACTIVE) {
            throw new IllegalArgumentException("Sản phẩm hoặc chi nhánh hiện không kinh doanh.");
        }
    }

    private void validatePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng phải là số nguyên dương.");
        }
    }

    private void validateAvailableQuantity(long requested, BranchInventory inventory) {
        int available = inventory.getAvailableQuantity();
        if (requested > available) {
            throw new IllegalArgumentException(
                    "Số lượng yêu cầu vượt tồn kho. Số lượng tối đa có thể mua: " + available + "."
            );
        }
    }

    private CartView toCartView(Cart cart) {
        List<CartLineView> lines = cart.getItems().stream()
                .map(item -> {
                    BranchInventory inventory = item.getInventory();
                    BigDecimal price = currentPrice(inventory);
                    return new CartLineView(
                            item.getId(),
                            inventory.getVariant().getProduct().getName(),
                            inventory.getVariant().getRam() + " / "
                                    + inventory.getVariant().getStorage() + " / "
                                    + inventory.getVariant().getColor(),
                            item.getQuantity(),
                            price,
                            price.multiply(BigDecimal.valueOf(item.getQuantity())),
                            inventory.getAvailableQuantity()
                    );
                })
                .toList();
        BigDecimal subtotal = lines.stream()
                .map(CartLineView::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartView(cart.getId(), cart.getBranch().getName(), lines, subtotal);
    }

    private BigDecimal currentPrice(BranchInventory inventory) {
        BigDecimal salePrice = inventory.getVariant().getSalePrice();
        return salePrice != null && salePrice.signum() > 0
                ? salePrice
                : inventory.getVariant().getPrice();
    }
}
