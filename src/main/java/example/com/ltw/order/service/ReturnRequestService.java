package example.com.ltw.order.service;

import example.com.ltw.order.entity.*;
import example.com.ltw.order.repository.*;
import example.com.ltw.shipping.entity.Shipment;
import example.com.ltw.shipping.repository.ShipmentRepository;
import example.com.ltw.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReturnRequestService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnRequestHistoryRepository historyRepository;
    private final ShipmentRepository shipmentRepository;

    @Value("${returns.allowed-days:0}")
    private int allowedDays;

    @Transactional
    public ReturnRequest submit(String orderCode, User customer, List<ReturnLineInput> requested, String reason) {
        Order order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (!order.getUser().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền tạo yêu cầu cho đơn hàng này.");
        }
        if (allowedDays <= 0) {
            throw new IllegalStateException("Chính sách thời hạn trả hàng chưa được cấu hình; hiện chưa thể tiếp nhận yêu cầu.");
        }
        Shipment shipment = shipmentRepository.findByOrderId(order.getId()).orElse(null);
        if (order.getStatus() != OrderStatus.DELIVERED || shipment == null || shipment.getDeliveredAt() == null
                || shipment.getDeliveredAt().plusDays(allowedDays).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Đơn hàng không đáp ứng thời hạn hoặc điều kiện trả hàng.");
        }
        if (requested == null || requested.isEmpty() || reason == null || reason.isBlank() || reason.length() > 1000) {
            throw new IllegalArgumentException("Cần chọn ít nhất một sản phẩm và nhập lý do trả hàng.");
        }

        Set<Long> seen = new HashSet<>();
        Map<Long, Integer> previouslyRequested = new HashMap<>();
        for (ReturnRequest old : returnRequestRepository.findAllByOrderIdOrderByCreatedAtDesc(order.getId())) {
            if (old.getStatus() != ReturnRequestStatus.REJECTED) {
                for (ReturnRequestItem line : old.getItems()) {
                    int reserved = old.getStatus() == ReturnRequestStatus.COMPLETED
                            ? line.getReceivedQuantity() : line.getRequestedQuantity();
                    previouslyRequested.merge(line.getOrderItem().getId(), reserved, Integer::sum);
                }
            }
        }
        ReturnRequest entity = new ReturnRequest();
        entity.setRequestCode("RET-" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase(Locale.ROOT));
        entity.setOrder(order);
        entity.setUser(customer);
        entity.setReason(reason.trim());
        for (ReturnLineInput input : requested) {
            if (input == null || input.orderItemId() == null || input.quantity() <= 0
                    || !seen.add(input.orderItemId())) {
                throw new IllegalArgumentException("Danh sách sản phẩm trả không hợp lệ.");
            }
            OrderItem item = orderItemRepository.findByIdAndOrderIdForUpdate(input.orderItemId(), order.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không thuộc đơn hàng này."));
            int alreadyRequested = previouslyRequested.getOrDefault(item.getId(), 0);
            if (input.quantity() + alreadyRequested > item.getQuantity()) {
                throw new IllegalArgumentException("Số lượng trả vượt số lượng còn có thể yêu cầu.");
            }
            ReturnRequestItem line = new ReturnRequestItem();
            line.setReturnRequest(entity);
            line.setOrderItem(item);
            line.setRequestedQuantity(input.quantity());
            entity.getItems().add(line);
        }
        ReturnRequest saved = returnRequestRepository.save(entity);
        history(saved, null, customer, "Khách hàng gửi yêu cầu trả hàng.");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ReturnRequest> customerRequests(Long userId) {
        return returnRequestRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    private void history(ReturnRequest request, ReturnRequestStatus previous, User actor, String note) {
        ReturnRequestHistory history = new ReturnRequestHistory();
        history.setReturnRequest(request);
        history.setPreviousStatus(previous == null ? null : previous.name());
        history.setNewStatus(request.getStatus().name());
        history.setChangedBy(actor);
        history.setNote(note);
        historyRepository.save(history);
    }

    public record ReturnLineInput(Long orderItemId, int quantity) {
    }
}
