package example.com.ltw.order.service;

import example.com.ltw.branch.entity.Branch;
import example.com.ltw.cart.entity.Cart;
import example.com.ltw.cart.entity.CartItem;
import example.com.ltw.cart.repository.CartItemRepository;
import example.com.ltw.cart.repository.CartRepository;
import example.com.ltw.catalog.entity.ProductVariant;
import example.com.ltw.inventory.entity.BranchInventory;
import example.com.ltw.order.entity.Order;
import example.com.ltw.order.entity.OrderItem;
import example.com.ltw.order.entity.OrderStatus;
import example.com.ltw.order.entity.PaymentMethod;
import example.com.ltw.order.entity.PaymentStatus;
import example.com.ltw.order.repository.OrderItemRepository;
import example.com.ltw.order.repository.OrderRepository;
import example.com.ltw.order.repository.OrderStatusHistoryRepository;
import example.com.ltw.inventory.repository.BranchInventoryRepository;
import example.com.ltw.inventory.repository.InventoryTransactionRepository;
import example.com.ltw.payment.repository.PaymentRepository;
import example.com.ltw.shipping.repository.ShipmentRepository;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderStatusHistoryRepository historyRepository;
    @Mock private BranchInventoryRepository inventoryRepository;
    @Mock private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private OrderService orderService;

    @Test
    void customerHistoryHidesFailedVnPayOrdersButKeepsOtherCancelledOrders() {
        Order failedVnPay = cancelledOrder(PaymentMethod.VNPAY, PaymentStatus.FAILED);
        Order latePaidVnPay = cancelledOrder(PaymentMethod.VNPAY, PaymentStatus.REFUND_REQUIRED);
        Order cancelledCod = cancelledOrder(PaymentMethod.COD, PaymentStatus.UNPAID);
        when(orderRepository.findAllByUserIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(failedVnPay, latePaidVnPay, cancelledCod));

        assertEquals(List.of(latePaidVnPay, cancelledCod), orderService.customerOrders(7L));
    }

    @Test
    void customerCancellationRestoresItemsToCartAndInventory() {
        User customer = org.mockito.Mockito.mock(User.class);
        Branch branch = org.mockito.Mockito.mock(Branch.class);
        ProductVariant variant = org.mockito.Mockito.mock(ProductVariant.class);
        BranchInventory inventory = org.mockito.Mockito.mock(BranchInventory.class);
        Order order = new Order();
        order.setId(12L);
        order.setOrderCode("NX12");
        order.setUser(customer);
        order.setBranch(branch);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        OrderItem orderItem = new OrderItem();
        orderItem.setProductVariant(variant);
        orderItem.setQuantity(2);
        Cart cart = new Cart();
        cart.setId(41L);

        when(customer.getId()).thenReturn(7L);
        when(branch.getId()).thenReturn(3L);
        when(variant.getId()).thenReturn(22L);
        when(orderRepository.findByOrderCodeForUpdate("NX12")).thenReturn(Optional.of(order));
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserIdAndBranchId(7L, 3L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(orderItemRepository.findByOrderIdOrderById(12L)).thenReturn(List.of(orderItem));
        when(inventoryRepository.findByBranchIdAndVariantIdForUpdate(3L, 22L))
                .thenReturn(Optional.of(inventory));
        when(inventory.getQuantity()).thenReturn(4);
        when(inventory.getId()).thenReturn(55L);
        when(inventory.getVariant()).thenReturn(variant);
        when(inventory.getBranch()).thenReturn(branch);
        when(cartItemRepository.findByCartIdAndInventoryId(41L, 55L)).thenReturn(Optional.empty());
        when(paymentRepository.findByOrderIdForUpdate(12L)).thenReturn(Optional.empty());
        when(shipmentRepository.findByOrderId(12L)).thenReturn(Optional.empty());

        orderService.cancelByCustomer("NX12", customer);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(inventory).setQuantity(6);
        org.mockito.ArgumentCaptor<CartItem> cartItemCaptor =
                org.mockito.ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(cartItemCaptor.capture());
        assertEquals(2, cartItemCaptor.getValue().getQuantity());
    }

    private Order cancelledOrder(PaymentMethod method, PaymentStatus paymentStatus) {
        Order order = new Order();
        order.setPaymentMethod(method);
        order.setPaymentStatus(paymentStatus);
        order.setStatus(OrderStatus.CANCELLED);
        return order;
    }
}
