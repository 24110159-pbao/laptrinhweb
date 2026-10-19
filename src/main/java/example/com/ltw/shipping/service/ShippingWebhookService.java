package example.com.ltw.shipping.service;

import example.com.ltw.order.entity.Order;
import example.com.ltw.order.entity.OrderStatus;
import example.com.ltw.order.entity.OrderStatusHistory;
import example.com.ltw.order.repository.OrderRepository;
import example.com.ltw.order.repository.OrderStatusHistoryRepository;
import example.com.ltw.shipping.entity.Shipment;
import example.com.ltw.shipping.entity.ShipmentEvent;
import example.com.ltw.shipping.entity.ShipmentStatus;
import example.com.ltw.shipping.repository.ShipmentEventRepository;
import example.com.ltw.shipping.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class ShippingWebhookService {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository eventRepository;
    private final OrderStatusHistoryRepository historyRepository;

    @Value("${shipping.webhook-secret:}")
    private String webhookSecret;

    public boolean verifySignature(String rawBody, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank() || rawBody == null
                || signature == null || signature.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            byte[] actual = HexFormat.of().parseHex(signature.trim());
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException | java.security.GeneralSecurityException ex) {
            return false;
        }
    }

    @Transactional
    public void handle(String eventId, String ghnOrderCode, String status) {
        if (eventId == null || eventId.isBlank() || eventId.length() > 100
                || ghnOrderCode == null || ghnOrderCode.isBlank() || ghnOrderCode.length() > 100
                || status == null || status.length() > 30) {
            throw new IllegalArgumentException("Dữ liệu callback vận chuyển không hợp lệ.");
        }
        Shipment shipmentRef = shipmentRepository.findByGhnOrderCode(ghnOrderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vận đơn."));
        Order order = orderRepository.findByIdForUpdate(shipmentRef.getOrder().getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (eventRepository.existsByEventId(eventId)) return;
        Shipment shipment = shipmentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng chưa có vận chuyển."));
        if (!ghnOrderCode.equals(shipment.getGhnOrderCode())) {
            throw new IllegalArgumentException("Mã vận đơn không khớp.");
        }
        ShipmentStatus eventStatus;
        try {
            eventStatus = ShipmentStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Trạng thái vận chuyển không được hỗ trợ.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Không thể thay đổi trạng thái vận chuyển của đơn đã hủy.");
        }
        OrderStatus previousOrderStatus = order.getStatus();
        switch (eventStatus) {
            case RECEIVED -> {
                if (order.getStatus() == OrderStatus.CONFIRMED) {
                    order.setStatus(OrderStatus.SHIPPING);
                } else if (order.getStatus() != OrderStatus.SHIPPING) {
                    throw new IllegalArgumentException("Đơn chưa ở trạng thái hợp lệ để bàn giao vận chuyển.");
                }
                shipment.setStatus(ShipmentStatus.RECEIVED);
            }
            case DELIVERED -> {
                if (order.getStatus() == OrderStatus.SHIPPING) {
                    order.setStatus(OrderStatus.DELIVERED);
                    shipment.setStatus(ShipmentStatus.DELIVERED);
                    shipment.setDeliveredAt(java.time.LocalDateTime.now());
                } else if (order.getStatus() != OrderStatus.DELIVERED) {
                    throw new IllegalArgumentException("Chỉ đơn đang giao mới được xác nhận đã giao.");
                }
            }
            case DELIVERY_FAILED -> {
                if (order.getStatus() != OrderStatus.SHIPPING) {
                    throw new IllegalArgumentException("Đơn chưa ở trạng thái đang giao.");
                }
                shipment.setStatus(ShipmentStatus.DELIVERY_FAILED);
            }
            case CANCELLED -> throw new IllegalArgumentException("Callback không được phép tự hủy đơn hàng.");
            case PENDING -> throw new IllegalArgumentException("Callback không được phép đưa vận đơn về trạng thái chờ.");
        }
        if (previousOrderStatus != order.getStatus()) {
            OrderStatusHistory history = new OrderStatusHistory();
            history.setOrder(order);
            history.setPreviousStatus(previousOrderStatus.name());
            history.setNewStatus(order.getStatus().name());
            history.setNote("Cập nhật trạng thái từ callback vận chuyển: " + eventStatus.name());
            historyRepository.save(history);
        }
        ShipmentEvent event = new ShipmentEvent();
        event.setShipment(shipment);
        event.setEventId(eventId);
        event.setStatus(eventStatus.name());
        eventRepository.save(event);
    }
}
