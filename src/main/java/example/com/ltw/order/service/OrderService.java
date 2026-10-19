package example.com.ltw.order.service;

import example.com.ltw.inventory.entity.BranchInventory;
import example.com.ltw.inventory.entity.InventoryTransaction;
import example.com.ltw.inventory.entity.InventoryTransactionType;
import example.com.ltw.inventory.repository.BranchInventoryRepository;
import example.com.ltw.inventory.repository.InventoryTransactionRepository;
import example.com.ltw.order.entity.Order;
import example.com.ltw.order.entity.OrderItem;
import example.com.ltw.order.entity.OrderStatus;
import example.com.ltw.order.entity.OrderStatusHistory;
import example.com.ltw.order.entity.PaymentStatus;
import example.com.ltw.order.repository.OrderItemRepository;
import example.com.ltw.order.repository.OrderRepository;
import example.com.ltw.order.repository.OrderStatusHistoryRepository;
import example.com.ltw.payment.entity.Payment;
import example.com.ltw.payment.entity.TransactionStatus;
import example.com.ltw.payment.repository.PaymentRepository;
import example.com.ltw.shipping.entity.Shipment;
import example.com.ltw.shipping.entity.ShipmentStatus;
import example.com.ltw.shipping.repository.ShipmentRepository;
import example.com.ltw.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final BranchInventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final ShipmentRepository shipmentRepository;

    @Transactional(readOnly = true)
    public List<Order> customerOrders(Long userId) {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Order customerOrder(String orderCode, Long userId) {
        return orderRepository.findWithBranchByOrderCodeAndUserId(orderCode, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
    }

    @Transactional
    public void cancelByCustomer(String orderCode, User customer) {
        Order order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (!order.getUser().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền hủy đơn hàng này.");
        }
        cancelLocked(order, customer, "Khách hàng yêu cầu hủy đơn.");
    }

    private void cancelLocked(Order order, User actor, String note) {
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException("Chỉ được hủy đơn trước khi chuyển sang trạng thái đang giao.");
        }
        OrderStatus previous = order.getStatus();
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderById(order.getId());
        for (OrderItem item : items.stream()
                .sorted((left, right) -> left.getProductVariant().getId()
                        .compareTo(right.getProductVariant().getId())).toList()) {
            BranchInventory inventory = inventoryRepository.findByBranchIdAndVariantIdForUpdate(
                    order.getBranch().getId(), item.getProductVariant().getId())
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy tồn kho để hoàn lại đơn."));
            int before = inventory.getQuantity();
            int after = Math.addExact(before, item.getQuantity());
            inventory.setQuantity(after);
            inventoryTransactionRepository.save(InventoryTransaction.create(
                    inventory.getVariant(), inventory.getBranch(), InventoryTransactionType.ORDER_RELEASED,
                    before, after, order.getOrderCode(), "Hoàn tồn kho do hủy đơn.", actor
            ));
        }
        Payment payment = paymentRepository.findByOrderIdForUpdate(order.getId()).orElse(null);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUND_REQUIRED);
        } else if (order.getPaymentMethod() == example.com.ltw.order.entity.PaymentMethod.VNPAY) {
            if (payment == null) {
                throw new IllegalStateException("Thiếu giao dịch VNPay; không thể hủy an toàn.");
            }
            if (payment.getStatus() == TransactionStatus.SUCCESS) {
                order.setPaymentStatus(PaymentStatus.REFUND_REQUIRED);
            } else {
                payment.setStatus(TransactionStatus.FAILED);
                payment.setResponseCode("CANCELLED");
                order.setPaymentStatus(PaymentStatus.FAILED);
            }
        } else if (payment != null && payment.getStatus() == TransactionStatus.PENDING) {
            payment.setStatus(TransactionStatus.FAILED);
            payment.setResponseCode("CANCELLED");
        }
        order.setStatus(OrderStatus.CANCELLED);
        shipmentRepository.findByOrderId(order.getId()).ifPresent(shipment ->
                shipment.setStatus(ShipmentStatus.CANCELLED));
        saveHistory(order, previous, actor, note);
    }

    private void saveHistory(Order order, OrderStatus previous, User actor, String note) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setPreviousStatus(previous.name());
        history.setNewStatus(order.getStatus().name());
        history.setChangedBy(actor);
        history.setNote(note);
        historyRepository.save(history);
    }

}
