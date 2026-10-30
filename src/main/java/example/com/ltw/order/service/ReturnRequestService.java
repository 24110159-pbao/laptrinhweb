package example.com.ltw.order.service;

import example.com.ltw.inventory.entity.BranchInventory;
import example.com.ltw.inventory.entity.InventoryTransaction;
import example.com.ltw.inventory.entity.InventoryTransactionType;
import example.com.ltw.inventory.repository.BranchInventoryRepository;
import example.com.ltw.inventory.repository.InventoryTransactionRepository;
import example.com.ltw.order.entity.*;
import example.com.ltw.order.repository.*;
import example.com.ltw.shipping.entity.Shipment;
import example.com.ltw.shipping.repository.ShipmentRepository;
import example.com.ltw.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnRequestService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnRequestHistoryRepository historyRepository;
    private final ShipmentRepository shipmentRepository;
    private final BranchInventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    @Value("${returns.allowed-days:7}")
    private int allowedDays;

    @Transactional
    public ReturnRequest submit(String orderCode, User customer, List<ReturnLineInput> requested, String reason) {
        Order order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (!order.getUser().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền tạo yêu cầu cho đơn hàng này.");
        }
        int effectiveAllowedDays = allowedDays > 0 ? allowedDays : 7;
        Shipment shipment = shipmentRepository.findByOrderId(order.getId()).orElse(null);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Đơn hàng chưa giao thành công, không thể yêu cầu trả hàng.");
        }
        if (shipment != null && shipment.getDeliveredAt() != null
                && shipment.getDeliveredAt().plusDays(effectiveAllowedDays).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Đơn hàng đã quá thời hạn trả hàng cho phép (" + effectiveAllowedDays + " ngày).");
        }
        if (requested == null || requested.isEmpty() || reason == null || reason.isBlank() || reason.length() > 1000) {
            throw new IllegalArgumentException("Cần chọn ít nhất một sản phẩm và nhập lý do trả hàng.");
        }

        List<ReturnRequest> existingRequests = returnRequestRepository.findAllByOrderIdOrderByCreatedAtDesc(order.getId());
        if (!existingRequests.isEmpty()) {
            throw new IllegalStateException("Đơn hàng này đã có yêu cầu hoàn hàng. Mỗi đơn hàng chỉ được gửi yêu cầu 1 lần để đợi nhân viên xét duyệt.");
        }

        Set<Long> seen = new HashSet<>();
        Map<Long, Integer> previouslyRequested = new HashMap<>();
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
        history(saved, null, customer, "Khách hàng gửi yêu cầu trả hàng: " + reason.trim());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ReturnRequest> customerRequests(Long userId) {
        return returnRequestRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<ReturnRequest> getBranchReturnRequests(Long branchId, ReturnRequestStatus status) {
        if (status != null) {
            return returnRequestRepository.findAllByOrder_Branch_IdAndStatusOrderByCreatedAtDesc(branchId, status);
        }
        return returnRequestRepository.findAllByOrder_Branch_IdOrderByCreatedAtDesc(branchId);
    }

    @Transactional(readOnly = true)
    public ReturnRequest getReturnRequestDetail(Long requestId, Long branchId) {
        ReturnRequest request = returnRequestRepository.findWithItemsById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu hoàn trả #" + requestId));
        if (branchId != null && request.getOrder().getBranch() != null
                && !request.getOrder().getBranch().getId().equals(branchId)) {
            throw new IllegalArgumentException("Bạn không có quyền quản lý yêu cầu hoàn trả của chi nhánh khác.");
        }
        return request;
    }

    @Transactional
    public ReturnRequest approveReturnRequest(Long requestId, User staff, String staffNote, boolean restockInventory) {
        ReturnRequest request = returnRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu hoàn trả #" + requestId));

        if (request.getStatus() != ReturnRequestStatus.PENDING_REVIEW && request.getStatus() != ReturnRequestStatus.AWAITING_RETURN) {
            throw new IllegalStateException("Yêu cầu hoàn trả này đã được xử lý từ trước (Trạng thái: " + request.getStatus() + ")");
        }

        ReturnRequestStatus previous = request.getStatus();
        request.setStatus(ReturnRequestStatus.APPROVED);
        request.setReviewedBy(staff);
        request.setStaffNote(staffNote != null ? staffNote.trim() : "Đã duyệt yêu cầu hoàn trả.");
        request.setInspectedAt(LocalDateTime.now());

        BigDecimal totalRefund = BigDecimal.ZERO;
        for (ReturnRequestItem line : request.getItems()) {
            int qty = line.getRequestedQuantity();
            line.setApprovedQuantity(qty);
            line.setAcceptedQuantity(qty);
            BigDecimal lineAmount = line.getOrderItem().getUnitPrice().multiply(BigDecimal.valueOf(qty));
            line.setRefundAmount(lineAmount);
            totalRefund = totalRefund.add(lineAmount);

            // Tự động nhập kho lại cho chi nhánh nếu chọn restock
            if (restockInventory && line.getOrderItem().getProductVariant() != null && inventoryRepository != null) {
                Long branchId = request.getOrder().getBranch().getId();
                Long variantId = line.getOrderItem().getProductVariant().getId();
                inventoryRepository.findByBranchIdAndVariantIdForUpdate(branchId, variantId).ifPresent(inventory -> {
                    int before = inventory.getQuantity();
                    int after = Math.addExact(before, qty);
                    inventory.setQuantity(after);
                    inventoryRepository.save(inventory);

                    if (inventoryTransactionRepository != null) {
                        inventoryTransactionRepository.save(InventoryTransaction.create(
                                inventory.getVariant(),
                                inventory.getBranch(),
                                InventoryTransactionType.STOCK_IN,
                                before,
                                after,
                                request.getRequestCode(),
                                "Hoàn trả hàng duyệt bởi nhân viên " + staff.getFullName() + ": " + (staffNote != null ? staffNote : ""),
                                staff
                        ));
                    }
                    log.info("Staff '{}' duyệt hoàn trả và nhập +{} sp variant #{} vào chi nhánh '{}'",
                            staff.getEmail(), qty, variantId, inventory.getBranch().getName());
                });
            }
        }

        request.setRefundAmount(totalRefund);
        request.setRefundStatus(RefundStatus.SUCCEEDED);
        request.setRefundedAt(LocalDateTime.now());

        // Cập nhật đơn hàng sang RETURN_REFUNDED
        Order order = request.getOrder();
        order.setStatus(OrderStatus.RETURN_REFUNDED);
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        String audit = "[NV duyệt hoàn trả #" + request.getRequestCode() + ": hoàn " + totalRefund + "đ]";
        order.setNote(order.getNote() != null ? audit + " | " + order.getNote() : audit);
        orderRepository.save(order);

        ReturnRequest saved = returnRequestRepository.save(request);
        String historyNote = "Nhân viên " + staff.getFullName() + " duyệt yêu cầu hoàn trả. Số tiền hoàn: "
                + totalRefund + "đ" + (restockInventory ? " (Đã cộng lại tồn kho chi nhánh)" : "")
                + (staffNote != null && !staffNote.isBlank() ? " | Ghi chú: " + staffNote.trim() : "");
        history(saved, previous, staff, historyNote);

        log.info("Nhân viên '{}' đã duyệt hoàn trả '{}' cho đơn '{}'", staff.getEmail(), saved.getRequestCode(), order.getOrderCode());
        return saved;
    }

    @Transactional
    public ReturnRequest rejectReturnRequest(Long requestId, User staff, String rejectReason) {
        ReturnRequest request = returnRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu hoàn trả #" + requestId));

        if (request.getStatus() != ReturnRequestStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Yêu cầu hoàn trả này đã được xử lý từ trước!");
        }
        if (rejectReason == null || rejectReason.trim().isBlank()) {
            throw new IllegalArgumentException("Vui lòng cung cấp lý do từ chối yêu cầu hoàn trả.");
        }

        ReturnRequestStatus previous = request.getStatus();
        request.setStatus(ReturnRequestStatus.REJECTED);
        request.setReviewedBy(staff);
        request.setStaffNote(rejectReason.trim());

        ReturnRequest saved = returnRequestRepository.save(request);
        history(saved, previous, staff, "Nhân viên " + staff.getFullName() + " từ chối yêu cầu. Lý do: " + rejectReason.trim());

        log.warn("Nhân viên '{}' đã từ chối yêu cầu hoàn trả '{}'. Lý do: {}", staff.getEmail(), saved.getRequestCode(), rejectReason);
        return saved;
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
