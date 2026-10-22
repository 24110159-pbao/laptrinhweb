package example.com.ltw.checkout.service;

import example.com.ltw.branch.entity.Branch;
import example.com.ltw.cart.entity.Cart;
import example.com.ltw.cart.entity.CartItem;
import example.com.ltw.cart.repository.CartItemRepository;
import example.com.ltw.cart.repository.CartRepository;
import example.com.ltw.catalog.entity.ProductVariant;
import example.com.ltw.checkout.repository.CheckoutSessionRepository;
import example.com.ltw.checkout.repository.InventoryReservationRepository;
import example.com.ltw.discount.repository.DiscountCodeRepository;
import example.com.ltw.discount.repository.DiscountUsageRepository;
import example.com.ltw.inventory.entity.BranchInventory;
import example.com.ltw.inventory.repository.BranchInventoryRepository;
import example.com.ltw.inventory.repository.InventoryTransactionRepository;
import example.com.ltw.order.entity.Order;
import example.com.ltw.order.entity.OrderItem;
import example.com.ltw.order.entity.OrderStatus;
import example.com.ltw.order.entity.PaymentMethod;
import example.com.ltw.order.entity.PaymentStatus;
import example.com.ltw.order.repository.OrderItemRepository;
import example.com.ltw.order.repository.OrderRepository;
import example.com.ltw.order.repository.OrderStatusHistoryRepository;
import example.com.ltw.payment.entity.Payment;
import example.com.ltw.payment.entity.TransactionStatus;
import example.com.ltw.payment.repository.PaymentRepository;
import example.com.ltw.payment.service.VnPayService;
import example.com.ltw.shipping.repository.ShipmentRepository;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceVnPayCancellationTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private CheckoutSessionRepository checkoutRepository;
    @Mock private InventoryReservationRepository reservationRepository;
    @Mock private BranchInventoryRepository inventoryRepository;
    @Mock private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private CheckoutAddressService checkoutAddressService;
    @Mock private DiscountCodeRepository discountCodeRepository;
    @Mock private DiscountUsageRepository discountUsageRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderStatusHistoryRepository orderHistoryRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private VnPayService vnPayService;

    @InjectMocks private CheckoutService checkoutService;

    @Test
    void cancelledVnPayOrderRestoresInventoryAndMergesProductsIntoBranchCart() {
        User user = org.mockito.Mockito.mock(User.class);
        Branch branch = org.mockito.Mockito.mock(Branch.class);
        ProductVariant variant = org.mockito.Mockito.mock(ProductVariant.class);
        BranchInventory inventory = org.mockito.Mockito.mock(BranchInventory.class);
        Cart cart = new Cart();
        cart.setId(41L);
        Order order = new Order();
        order.setId(12L);
        order.setOrderCode("NX12");
        order.setUser(user);
        order.setBranch(branch);
        order.setPaymentMethod(PaymentMethod.VNPAY);
        order.setPaymentStatus(PaymentStatus.PENDING);
        OrderItem orderItem = new OrderItem();
        orderItem.setProductVariant(variant);
        orderItem.setQuantity(2);
        Payment payment = new Payment();
        payment.setStatus(TransactionStatus.PENDING);

        when(user.getId()).thenReturn(7L);
        when(branch.getId()).thenReturn(3L);
        when(variant.getId()).thenReturn(22L);
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdAndBranchId(7L, 3L)).thenReturn(Optional.of(cart));
        when(orderItemRepository.findByOrderIdOrderById(12L)).thenReturn(List.of(orderItem));
        when(inventoryRepository.findByBranchIdAndVariantIdForUpdate(3L, 22L))
                .thenReturn(Optional.of(inventory));
        when(inventory.getQuantity()).thenReturn(5);
        when(inventory.getId()).thenReturn(55L);
        when(inventory.getVariant()).thenReturn(variant);
        when(inventory.getBranch()).thenReturn(branch);
        when(cartItemRepository.findByCartIdAndInventoryId(41L, 55L)).thenReturn(Optional.empty());
        when(shipmentRepository.findByOrderId(12L)).thenReturn(Optional.empty());

        checkoutService.cancelUnpaidVnPayOrder(order, payment, "24", "Customer cancelled payment");

        verify(inventory).setQuantity(7);
        ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(itemCaptor.capture());
        assertEquals(2, itemCaptor.getValue().getQuantity());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
        assertEquals(TransactionStatus.FAILED, payment.getStatus());
        assertEquals("24", payment.getResponseCode());
        verify(inventoryTransactionRepository).save(any());
    }
}
