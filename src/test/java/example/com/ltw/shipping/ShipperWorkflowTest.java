package example.com.ltw.shipping;

import example.com.ltw.branch.entity.Branch;
import example.com.ltw.order.entity.Order;
import example.com.ltw.order.entity.OrderStatus;
import example.com.ltw.order.repository.OrderItemRepository;
import example.com.ltw.order.repository.OrderRepository;
import example.com.ltw.order.service.BranchOrderWorkflowService;
import example.com.ltw.shipping.entity.Shipment;
import example.com.ltw.shipping.entity.ShipmentStatus;
import example.com.ltw.shipping.repository.ShipmentRepository;
import example.com.ltw.shipping.service.GhnShippingService;
import example.com.ltw.user.entity.Role;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ShipperWorkflowTest {

    private ShipmentRepository shipmentRepository;
    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private UserRepository userRepository;
    private GhnShippingService ghnShippingService;
    private BranchOrderWorkflowService workflowService;

    private User shipper;
    private Shipment shipment;
    private Order order;

    @BeforeEach
    void setUp() {
        shipmentRepository = mock(ShipmentRepository.class);
        orderRepository = mock(OrderRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        userRepository = mock(UserRepository.class);
        ghnShippingService = mock(GhnShippingService.class);

        workflowService = new BranchOrderWorkflowService(
                orderRepository,
                orderItemRepository,
                shipmentRepository,
                userRepository,
                ghnShippingService
        );

        shipper = new User();
        shipper.setId(7L);
        shipper.setEmail("shipper1@example.com");
        shipper.setFullName("Shipper 1");
        shipper.setRole(Role.SHIPPER);

        order = new Order();
        order.setId(100L);
        order.setOrderCode("ORD100");
        order.setStatus(OrderStatus.SHIPPING);

        shipment = new Shipment();
        shipment.setId(200L);
        shipment.setShipper(shipper);
        shipment.setOrder(order);
        shipment.setStatus(ShipmentStatus.RECEIVED);
        shipment.setAttemptCount(0);

        when(shipmentRepository.findById(200L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void testFailDelivery_CustomerCancelOrReturn_CancelsOrderImmediately() {
        Shipment result = workflowService.failDelivery(shipper, 200L, "Khách từ chối nhận hàng do đổi ý, đòi hủy đơn");

        assertEquals(ShipmentStatus.DELIVERY_FAILED, result.getStatus());
        assertEquals(1, result.getAttemptCount());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertTrue(order.getNote().contains("Khách hủy/hoàn hàng"));

        // Retry should be forbidden
        assertThrows(IllegalStateException.class, () -> workflowService.retryDelivery(shipper, 200L));
    }

    @Test
    void testFailDelivery_NormalReason_AllowsRetryBefore3Attempts() {
        // Attempt 1
        Shipment r1 = workflowService.failDelivery(shipper, 200L, "Khách không nghe máy");
        assertEquals(ShipmentStatus.DELIVERY_FAILED, r1.getStatus());
        assertEquals(1, r1.getAttemptCount());
        assertEquals(OrderStatus.SHIPPING, order.getStatus());
        assertTrue(order.getNote().contains("lần 1/3"));

        // Retry succeeds
        Shipment retry1 = workflowService.retryDelivery(shipper, 200L);
        assertEquals(ShipmentStatus.RECEIVED, retry1.getStatus());
        assertEquals(OrderStatus.SHIPPING, order.getStatus());

        // Attempt 2
        Shipment r2 = workflowService.failDelivery(shipper, 200L, "Khách hẹn lại ngày mai");
        assertEquals(2, r2.getAttemptCount());
        assertEquals(OrderStatus.SHIPPING, order.getStatus());

        // Retry succeeds
        Shipment retry2 = workflowService.retryDelivery(shipper, 200L);
        assertEquals(ShipmentStatus.RECEIVED, retry2.getStatus());

        // Attempt 3
        Shipment r3 = workflowService.failDelivery(shipper, 200L, "Sai địa chỉ không tìm thấy nhà");
        assertEquals(3, r3.getAttemptCount());
        assertEquals(OrderStatus.CANCELLED, order.getStatus()); // Cancelled after 3 failed attempts!
        assertTrue(order.getNote().contains("Quá 3 lần giao thất bại"));

        // Retry 3 fails
        assertThrows(IllegalStateException.class, () -> workflowService.retryDelivery(shipper, 200L));
    }
}
