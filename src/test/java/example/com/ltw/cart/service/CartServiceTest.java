package example.com.ltw.cart.service;

import example.com.ltw.branch.entity.Branch;
import example.com.ltw.branch.entity.BranchStatus;
import example.com.ltw.cart.entity.Cart;
import example.com.ltw.cart.entity.CartItem;
import example.com.ltw.cart.repository.CartItemRepository;
import example.com.ltw.cart.repository.CartRepository;
import example.com.ltw.checkout.repository.CheckoutSessionRepository;
import example.com.ltw.catalog.entity.Product;
import example.com.ltw.catalog.entity.ProductStatus;
import example.com.ltw.catalog.entity.ProductVariant;
import example.com.ltw.catalog.entity.ProductVariantStatus;
import example.com.ltw.inventory.entity.BranchInventory;
import example.com.ltw.inventory.repository.BranchInventoryRepository;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.repository.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private BranchInventoryRepository inventoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CheckoutSessionRepository checkoutRepository;
    @InjectMocks
    private CartService cartService;

    @Test
    void getCartsOmitsBranchesWithoutProducts() {
        Cart emptyCart = mock(Cart.class);
        Cart cartWithProducts = mock(Cart.class);
        CartItem cartItem = mock(CartItem.class);
        BranchInventory inventory = mock(BranchInventory.class);
        ProductVariant variant = mock(ProductVariant.class);
        Product product = mock(Product.class);
        when(emptyCart.getItems()).thenReturn(java.util.List.of());
        when(cartWithProducts.getItems()).thenReturn(List.of(cartItem));
        when(cartItem.getInventory()).thenReturn(inventory);
        when(inventory.getVariant()).thenReturn(variant);
        when(inventory.getAvailableQuantity()).thenReturn(5);
        when(variant.getProduct()).thenReturn(product);
        when(variant.getRam()).thenReturn("8GB");
        when(variant.getStorage()).thenReturn("256GB");
        when(variant.getColor()).thenReturn("Black");
        when(variant.getPrice()).thenReturn(BigDecimal.TEN);
        when(product.getName()).thenReturn("Phone");
        when(cartRepository.findAllByUserIdOrderByUpdatedAtDesc(7L))
                .thenReturn(List.of(emptyCart, cartWithProducts));
        Branch branch = mock(Branch.class);
        when(cartWithProducts.getBranch()).thenReturn(branch);
        when(branch.getName()).thenReturn("Chi nhánh A");

        var carts = cartService.getCarts(7L);

        assertEquals(1, carts.size());
        assertEquals("Chi nhánh A", carts.get(0).branchName());
    }

    @Test
    void addItemAccumulatesMatchingBranchVariantWithinAvailableStock() {
        User user = mock(User.class);
        Branch branch = activeBranch();
        BranchInventory inventory = activeInventory(branch, 5);
        Cart cart = mock(Cart.class);
        CartItem item = mock(CartItem.class);
        when(inventory.getId()).thenReturn(22L);
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(inventoryRepository.findByIdForUpdate(22L)).thenReturn(Optional.of(inventory));
        when(cartRepository.findByUserIdAndBranchId(7L, 3L)).thenReturn(Optional.of(cart));
        when(cart.getId()).thenReturn(11L);
        when(cartItemRepository.findByCartIdAndInventoryId(11L, 22L)).thenReturn(Optional.of(item));
        when(item.getQuantity()).thenReturn(2);

        cartService.addItem(7L, 22L, 2);

        verify(item).setQuantity(4);
        verify(cartItemRepository).save(item);
    }

    @Test
    void addItemRejectsQuantityAboveAvailableAndReportsMaximum() {
        User user = mock(User.class);
        Branch branch = activeBranch();
        BranchInventory inventory = activeInventory(branch, 3);
        when(inventory.getId()).thenReturn(22L);
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(inventoryRepository.findByIdForUpdate(22L)).thenReturn(Optional.of(inventory));
        when(cartRepository.findByUserIdAndBranchId(7L, 3L)).thenReturn(Optional.empty());
        Cart newCart = mock(Cart.class);
        when(newCart.getId()).thenReturn(11L);
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);
        when(cartItemRepository.findByCartIdAndInventoryId(11L, 22L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addItem(7L, 22L, 4)
        );

        assertEquals(
                "Số lượng yêu cầu vượt tồn kho. Số lượng tối đa có thể mua: 3.",
                exception.getMessage()
        );
        verify(cartItemRepository, never()).save(any());
    }

    private Branch activeBranch() {
        Branch branch = mock(Branch.class);
        when(branch.getId()).thenReturn(3L);
        when(branch.getStatus()).thenReturn(BranchStatus.ACTIVE);
        return branch;
    }

    private BranchInventory activeInventory(Branch branch, int available) {
        Product product = mock(Product.class);
        ProductVariant variant = mock(ProductVariant.class);
        BranchInventory inventory = mock(BranchInventory.class);
        when(inventory.getBranch()).thenReturn(branch);
        when(inventory.getVariant()).thenReturn(variant);
        when(inventory.getAvailableQuantity()).thenReturn(available);
        when(variant.getStatus()).thenReturn(ProductVariantStatus.ACTIVE);
        when(variant.getProduct()).thenReturn(product);
        when(product.getStatus()).thenReturn(ProductStatus.ACTIVE);
        return inventory;
    }
}
