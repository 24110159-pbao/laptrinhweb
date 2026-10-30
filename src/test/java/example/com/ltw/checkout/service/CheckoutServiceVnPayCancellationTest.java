package example.com.ltw.checkout.service;

import example.com.ltw.branch.entity.Branch;
import example.com.ltw.cart.entity.Cart;
import example.com.ltw.cart.entity.CartItem;
import example.com.ltw.cart.repository.CartItemRepository;
import example.com.ltw.cart.repository.CartRepository;
import example.com.ltw.branch.entity.BranchStatus;
import example.com.ltw.catalog.entity.Product;
import example.com.ltw.catalog.entity.ProductStatus;
import example.com.ltw.catalog.entity.ProductVariant;
import example.com.ltw.catalog.entity.ProductVariantStatus;
import example.com.ltw.checkout.entity.CheckoutStatus;
import example.com.ltw.checkout.entity.CheckoutSession;
import example.com.ltw.checkout.entity.InventoryReservation;
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
import example.com.ltw.shipping.repository.ShipmentEventRepository;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
    @Mock private ShipmentEventRepository shipmentEventRepository;
    @Mock private VnPayService vnPayService;
    @Mock private example.com.ltw.chat.service.ChatService chatService;

    @InjectMocks private CheckoutService checkoutService;

    @Test
    void startingVnPayDoesNotCreateAnOrderOrClearTheCart() {
        User user = org.mockito.Mockito.mock(User.class);
        Branch branch = org.mockito.Mockito.mock(Branch.class);
        Product product = org.mockito.Mockito.mock(Product.class);
        ProductVariant variant = org.mockito.Mockito.mock(ProductVariant.class);
        BranchInventory inventory = org.mockito.Mockito.mock(BranchInventory.class);
        Cart cart = new Cart();
        cart.setId(41L);
        CartItem cartItem = new CartItem();
        cartItem.setQuantity(1);
        cartItem.setInventory(inventory);
        cart.setItems(List.of(cartItem));
        CheckoutSession checkout = new CheckoutSession();
        checkout.setId(8L);
        checkout.setOrderCode("NX8");
        checkout.setCart(cart);
        checkout.setUser(user);
        checkout.setStatus(CheckoutStatus.ACTIVE);
        checkout.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        checkout.setReceiverName("Nguyen Van A");
        checkout.setSubtotal(new BigDecimal("100"));
        checkout.setFinalAmount(new BigDecimal("100"));

        InventoryReservation reservation = new InventoryReservation();
        reservation.setQuantity(1);
        reservation.setExpiresAt(checkout.getExpiresAt());
        reservation.setUnitPrice(new BigDecimal("100"));
        when(user.getId()).thenReturn(7L);
        when(branch.getStatus()).thenReturn(BranchStatus.ACTIVE);
        when(product.getStatus()).thenReturn(ProductStatus.ACTIVE);
        when(variant.getProduct()).thenReturn(product);
        when(variant.getStatus()).thenReturn(ProductVariantStatus.ACTIVE);
        when(variant.getPrice()).thenReturn(new BigDecimal("100"));
        when(inventory.getId()).thenReturn(55L);
        when(inventory.getBranch()).thenReturn(branch);
        when(inventory.getVariant()).thenReturn(variant);
        when(inventory.getQuantity()).thenReturn(5);
        when(inventory.getReservedQuantity()).thenReturn(1);
        when(checkoutRepository.findForUpdate(8L, 7L)).thenReturn(Optional.of(checkout));
        when(cartRepository.findByIdForUpdateAndUserId(41L, 7L)).thenReturn(Optional.of(cart));
        when(inventoryRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(inventory));
        when(reservationRepository.findByCheckoutSessionIdAndInventoryIdAndConvertedToOrderFalse(8L, 55L))
                .thenReturn(Optional.of(reservation));
        when(reservationRepository.findByCheckoutSessionAndConvertedToOrderFalse(checkout))
                .thenReturn(List.of(reservation));
        when(vnPayService.createPaymentUrl(
                eq("NX8"), eq(new BigDecimal("100")), any(), any(LocalDateTime.class)
        )).thenReturn("https://sandbox.example/pay");
        ReflectionTestUtils.setField(checkoutService, "vnPayTimeoutMinutes", 15);

        CheckoutService.FinalizeResult result = checkoutService.finalizeOrder(8L, 7L, PaymentMethod.VNPAY, "127.0.0.1");

        assertEquals("https://sandbox.example/pay", result.paymentUrl());
        assertEquals(CheckoutStatus.ACTIVE, checkout.getStatus());
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderItemRepository, never()).save(any(OrderItem.class));
        verify(cartItemRepository, never()).deleteByCartId(41L);
    }

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
        CheckoutSession checkoutSession = new CheckoutSession();
        order.setCheckoutSession(checkoutSession);
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
        assertEquals(CheckoutStatus.CANCELLED, checkoutSession.getStatus());
        verify(paymentRepository).delete(payment);
        verify(orderItemRepository).deleteByOrderId(12L);
        verify(orderHistoryRepository).deleteByOrderId(12L);
        verify(orderRepository).delete(order);
    }

    @Test
    void expiredVnPayOrderRestoresCartEvenWhenGatewayStatusCannotBeVerified() {
        User user = org.mockito.Mockito.mock(User.class);
        Branch branch = org.mockito.Mockito.mock(Branch.class);
        ProductVariant variant = org.mockito.Mockito.mock(ProductVariant.class);
        BranchInventory inventory = org.mockito.Mockito.mock(BranchInventory.class);
        Cart cart = new Cart();
        cart.setId(41L);
        Order order = new Order();
        order.setId(12L);
        order.setOrderCode("NX12");
        order.setCheckoutSession(new CheckoutSession());
        order.setUser(user);
        order.setBranch(branch);
        order.setPaymentMethod(PaymentMethod.VNPAY);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setPaymentExpiresAt(LocalDateTime.now().minusMinutes(1));
        OrderItem orderItem = new OrderItem();
        orderItem.setProductVariant(variant);
        orderItem.setQuantity(2);
        Payment payment = new Payment();
        payment.setStatus(TransactionStatus.PENDING);

        when(checkoutRepository.findByStatusAndExpiresAtLessThanEqualOrderById(
                eq(CheckoutStatus.ACTIVE), any(LocalDateTime.class))).thenReturn(List.of());
        when(orderRepository.findByPaymentStatusAndPaymentMethodAndPaymentExpiresAtLessThanEqual(
                eq(PaymentStatus.PENDING), eq(PaymentMethod.VNPAY), any(LocalDateTime.class)))
                .thenReturn(List.of(order));
        when(orderRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderIdForUpdate(12L)).thenReturn(Optional.of(payment));
        doThrow(new IllegalStateException("VNPay unavailable"))
                .when(vnPayService).queryTransaction(payment);
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

        checkoutService.expireCheckouts();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
        assertEquals(TransactionStatus.FAILED, payment.getStatus());
        assertEquals("TIMEOUT_UNVERIFIED", payment.getResponseCode());
        verify(inventory).setQuantity(7);
        verify(cartItemRepository).save(any(CartItem.class));
    }
}
