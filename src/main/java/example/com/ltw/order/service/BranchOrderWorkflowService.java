package example.com.ltw.order.service;

import example.com.ltw.branch.entity.Branch;
import example.com.ltw.order.entity.Order;
import example.com.ltw.order.entity.OrderItem;
import example.com.ltw.order.entity.OrderStatus;
import example.com.ltw.order.entity.PaymentMethod;
import example.com.ltw.order.entity.PaymentStatus;
import example.com.ltw.order.repository.OrderItemRepository;
import example.com.ltw.order.repository.OrderRepository;
import example.com.ltw.shipping.entity.Shipment;
import example.com.ltw.shipping.entity.ShipmentStatus;
import example.com.ltw.shipping.repository.ShipmentRepository;
import example.com.ltw.shipping.service.GhnShippingService;
import example.com.ltw.user.entity.Role;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Dịch vụ nghiệp vụ luồng đơn hàng và giao vận dùng chung (Shared Order & Fulfillment Workflow Service).
 * Đảm bảo Staff và Shipper cùng tuân thủ một chu trình nghiệp vụ khép kín:
 * 1. Khách đặt (PENDING)
 * 2. Staff duyệt (CONFIRMED)
 * 3. Staff đóng gói & Bàn giao Shipper (Sinh mã GHN, Shipment: PENDING)
 * 4. Shipper nhận hàng tại kho (Shipment: RECEIVED, Order: SHIPPING)
 * 5. Shipper giao thành công (Shipment: DELIVERED, Order: DELIVERED, COD -> PAID)
 *    hoặc Báo giao thất bại (Shipment: DELIVERY_FAILED)
 */
@Slf4j
@Service
public class BranchOrderWorkflowService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final GhnShippingService ghnShippingService;

    public BranchOrderWorkflowService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ShipmentRepository shipmentRepository,
            UserRepository userRepository,
            GhnShippingService ghnShippingService
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.shipmentRepository = shipmentRepository;
        this.userRepository = userRepository;
        this.ghnShippingService = ghnShippingService;
    }

    // ==========================================
    // 1. STAFF ACTIONS (XỬ LÝ ĐƠN TẠI CHI NHÁNH)
    // ==========================================

    /**
     * Bước 2: Staff chi nhánh xác nhận đơn hàng (PENDING -> CONFIRMED).
     * Ghi vết trách nhiệm của nhân viên vào trường note (không sửa DB).
     */
    @Transactional
    public Order confirmOrder(User staff, Long orderId) {
        Order order = getOrderForStaff(staff, orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Đơn hàng #" + order.getOrderCode() + " đã được nhân viên khác tiếp nhận hoặc không ở trạng thái chờ duyệt!");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setAssignedStaff(staff);
        LocalDateTime now = LocalDateTime.now();
        order.setUpdatedAt(now);

        // Lưu vết trách nhiệm nhân viên tiếp nhận đơn hàng (tận dụng trường note không sửa DB)
        String timeStr = now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
        String staffAudit = "[NV tiếp nhận: " + staff.getFullName() + " (" + staff.getEmail() + ") lúc " + timeStr + "]";
        String currentNote = order.getNote();
        if (currentNote == null || currentNote.isBlank()) {
            order.setNote(staffAudit);
        } else if (!currentNote.contains("[NV tiếp nhận:")) {
            order.setNote(staffAudit + " | " + currentNote);
        }

        Order saved = orderRepository.save(order);

        log.info("Staff '{}' ({}) đã xác nhận tiếp nhận đơn hàng '{}' tại chi nhánh '{}'",
                staff.getFullName(), staff.getEmail(), order.getOrderCode(), order.getBranch().getName());
        return saved;
    }

    /**
     * Bước 3: Staff bàn giao đơn hàng cho Shipper cùng chi nhánh & phát hành mã GHN.
     * Nếu shipperId là null hoặc <= 0, hệ thống tự động phân công theo thuật toán:
     * Shipper ít việc nhất -> Người chờ lâu nhất nhận trước.
     */
    @Transactional
    public Shipment assignShipper(User staff, Long orderId, Long shipperId, boolean generateGhn) {
        Order order = getOrderForStaff(staff, orderId);

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Không thể bàn giao vận chuyển cho đơn hàng đã hủy hoặc đã giao thành công!");
        }

        Branch branch = order.getBranch();
        User shipper;
        if (shipperId == null || shipperId <= 0) {
            // Tự động phân công tối ưu: Shipper ít đơn nhất, hòa thì người chờ lâu nhất nhận trước
            shipper = findOptimalShipper(branch.getId());
        } else {
            shipper = userRepository.findByIdAndBranch_IdAndRole(shipperId, branch.getId(), Role.SHIPPER)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Không tìm thấy Shipper hợp lệ trong cùng chi nhánh '" + branch.getName() + "'!"
                    ));
        }

        Shipment shipment = shipmentRepository.findByOrder_Id(orderId)
                .orElseGet(() -> {
                    Shipment s = new Shipment();
                    s.setOrder(order);
                    s.setShippingFee(order.getShippingFee());
                    return s;
                });

        shipment.setShipper(shipper);
        shipment.setStatus(ShipmentStatus.PENDING); // Chờ shipper nhận tại kho chi nhánh

        if (generateGhn || shipment.getGhnOrderCode() == null || shipment.getGhnOrderCode().isBlank()) {
            String ghnCode = ghnShippingService.generateGhnOrderCode(order);
            shipment.setGhnOrderCode(ghnCode);
            shipment.setExpectedDeliveryAt(ghnShippingService.calculateExpectedDeliveryTime());
        }

        Shipment savedShipment = shipmentRepository.save(shipment);

        // Khi nhân viên đóng gói xong và phân công bàn giao cho Shipper:
        // Đơn hàng lập tức chuyển sang trạng thái SHIPPING (Đang giao hàng)
        order.setStatus(OrderStatus.SHIPPING);
        if (order.getAssignedStaff() == null) {
            order.setAssignedStaff(staff);
        }
        LocalDateTime now = LocalDateTime.now();
        order.setUpdatedAt(now);

        // Ghi vết bàn giao Shipper vào note
        String assignAudit = "[NV bàn giao: " + staff.getFullName() + " -> Shipper: " + shipper.getFullName() + "]";
        String currentNote = order.getNote();
        if (currentNote == null || currentNote.isBlank()) {
            order.setNote(assignAudit);
        } else if (!currentNote.contains("[NV bàn giao:")) {
            order.setNote(assignAudit + " | " + currentNote);
        }
        orderRepository.save(order);

        log.info("Staff '{}' đã bàn giao đơn '{}' cho Shipper '{}' với mã GHN '{}'",
                staff.getEmail(), order.getOrderCode(), shipper.getFullName(), savedShipment.getGhnOrderCode());
        return savedShipment;
    }

    /**
     * Staff hủy đơn hàng (kèm lý do ghi log).
     */
    @Transactional
    public Order cancelOrder(User staff, Long orderId, String reason) {
        Order order = getOrderForStaff(staff, orderId);

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Không thể hủy đơn hàng đã giao thành công!");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Đơn hàng này đã bị hủy từ trước!");
        }
        if (order.getStatus() == OrderStatus.SHIPPING) {
            throw new IllegalStateException("Đơn hàng đang trên đường đi giao. Vui lòng liên hệ với Shipper để hoàn bưu phẩm về kho trước!");
        }

        String cancelLog = "[Staff hủy: " + (reason != null ? reason.trim() : "Không có lý do") + "]";
        String currentNote = order.getNote();
        order.setNote(currentNote != null && !currentNote.isBlank() ? cancelLog + " | " + currentNote : cancelLog);
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());

        // Hủy luôn shipment nếu có
        shipmentRepository.findByOrder_Id(orderId).ifPresent(s -> {
            s.setStatus(ShipmentStatus.CANCELLED);
            s.setUpdatedAt(LocalDateTime.now());
            shipmentRepository.save(s);
        });

        Order saved = orderRepository.save(order);
        log.warn("Staff '{}' đã hủy đơn '{}'. Lý do: {}", staff.getEmail(), order.getOrderCode(), reason);
        return saved;
    }

    // ==========================================
    // 2. SHIPPER ACTIONS (TIẾP NHẬN & GIAO VẬN)
    // ==========================================

    /**
     * Bước 4: Shipper nhận gói hàng tại kho chi nhánh (PENDING -> RECEIVED).
     * Kết nối API GHN (Giao Hàng Nhanh) để sinh mã vận đơn chính thức và cập nhật Order sang SHIPPING.
     */
    @Transactional
    public Shipment receivePackage(User shipper, Long shipmentId) {
        Shipment shipment = getShipmentForShipper(shipper, shipmentId);

        if (shipment.getStatus() != ShipmentStatus.PENDING) {
            throw new IllegalStateException("Gói hàng đã được tiếp nhận từ trước hoặc không ở trạng thái chờ nhận kho!");
        }

        LocalDateTime now = LocalDateTime.now();
        shipment.setStatus(ShipmentStatus.RECEIVED);
        shipment.setUpdatedAt(now);

        Order order = shipment.getOrder();
        order.setStatus(OrderStatus.SHIPPING); // Chuyển Order sang trạng thái Đang giao hàng
        order.setUpdatedAt(now);
        orderRepository.save(order);

        // Gọi API GHN để sinh mã vận đơn chính thức từ hệ thống Giao Hàng Nhanh
        List<OrderItem> items = orderItemRepository.findAllByOrder_IdOrderByIdAsc(order.getId());
        example.com.ltw.shipping.dto.GhnCreateOrderResponse ghnResp = ghnShippingService.createShippingOrder(order, items);
        if (ghnResp != null && ghnResp.getOrderCode() != null && !ghnResp.getOrderCode().isBlank()) {
            shipment.setGhnOrderCode(ghnResp.getOrderCode());
            if (ghnResp.getExpectedDeliveryTime() != null) {
                shipment.setExpectedDeliveryAt(ghnResp.getExpectedDeliveryTime());
            }
        }

        Shipment saved = shipmentRepository.save(shipment);
        log.info("Shipper '{}' đã nhận đơn '{}'. Mã GHN phát hành: '{}'",
                shipper.getEmail(), order.getOrderCode(), saved.getGhnOrderCode());
        return saved;
    }

    /**
     * Shipper giao lại đơn hàng (DELIVERY_FAILED -> RECEIVED).
     */
    @Transactional
    public Shipment retryDelivery(User shipper, Long shipmentId) {
        Shipment shipment = getShipmentForShipper(shipper, shipmentId);

        if (shipment.getStatus() != ShipmentStatus.DELIVERY_FAILED) {
            throw new IllegalStateException("Chỉ có thể giao lại đơn hàng đang ở trạng thái giao thất bại!");
        }

        Order order = shipment.getOrder();
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Đơn hàng này đã bị hủy (khách yêu cầu hoàn/hủy hoặc đã thất bại 3 lần), không thể kích hoạt giao lại!");
        }

        if (shipment.getAttemptCount() != null && shipment.getAttemptCount() >= 3) {
            throw new IllegalStateException("Đơn hàng đã vượt quá giới hạn 3 lần giao thất bại, không thể giao lại!");
        }

        LocalDateTime now = LocalDateTime.now();
        shipment.setStatus(ShipmentStatus.RECEIVED);
        shipment.setUpdatedAt(now);

        order.setStatus(OrderStatus.SHIPPING);
        order.setUpdatedAt(now);
        orderRepository.save(order);

        Shipment saved = shipmentRepository.save(shipment);
        log.info("Shipper '{}' kích hoạt giao lại cho đơn '{}' (Lần thử: {})", 
                shipper.getEmail(), order.getOrderCode(), (shipment.getAttemptCount() != null ? shipment.getAttemptCount() + 1 : 1));
        return saved;
    }

    /**
     * Cập nhật trạng thái giao vận thống nhất cho Shipper.
     */
    @Transactional
    public Shipment updateShipmentStatus(User shipper, Long shipmentId, ShipmentStatus newStatus, String reason) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Trạng thái mới không hợp lệ!");
        }
        return switch (newStatus) {
            case RECEIVED -> {
                Shipment s = getShipmentForShipper(shipper, shipmentId);
                if (s.getStatus() == ShipmentStatus.PENDING) {
                    yield receivePackage(shipper, shipmentId);
                } else if (s.getStatus() == ShipmentStatus.DELIVERY_FAILED) {
                    yield retryDelivery(shipper, shipmentId);
                } else {
                    yield s;
                }
            }
            case DELIVERED -> completeDelivery(shipper, shipmentId);
            case DELIVERY_FAILED -> failDelivery(shipper, shipmentId, reason);
            default -> throw new IllegalArgumentException("Không hỗ trợ chuyển sang trạng thái: " + newStatus);
        };
    }

    /**
     * Bước 5: Shipper xác nhận giao hàng thành công (RECEIVED -> DELIVERED).
     * Nếu là thanh toán COD, tự động cập nhật PaymentStatus sang PAID.
     */
    @Transactional
    public Shipment completeDelivery(User shipper, Long shipmentId) {
        Shipment shipment = getShipmentForShipper(shipper, shipmentId);

        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new IllegalStateException("Đơn hàng này đã được xác nhận giao thành công từ trước!");
        }

        LocalDateTime now = LocalDateTime.now();
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(now);
        shipment.setUpdatedAt(now);

        Order order = shipment.getOrder();
        order.setStatus(OrderStatus.DELIVERED);
        order.setUpdatedAt(now);

        if (order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.PAID);
            log.info("Shipper '{}' đã thu tiền mặt COD cho đơn '{}' ({})",
                    shipper.getEmail(), order.getOrderCode(), order.getFinalAmount());
        }

        orderRepository.save(order);
        Shipment saved = shipmentRepository.save(shipment);
        log.info("Shipper '{}' đã hoàn tất giao hàng thành công cho đơn '{}'", shipper.getEmail(), order.getOrderCode());
        return saved;
    }

    /**
     * Bước 5 (Nhánh lỗi): Shipper báo giao hàng thất bại kèm lý do cụ thể.
     * Quy tắc chuẩn hóa:
     * - Nếu khách đòi hoàn hàng hoặc hủy đơn: chuyển giao thất bại và cập nhật order sang CANCELLED (không cho giao lại).
     * - Nếu lý do bình thường: ghi nhận số lần thất bại (1, 2, 3). Nếu thất bại đủ 3 lần thì tự động chuyển sang CANCELLED (hủy đơn hoàn kho).
     */
    @Transactional
    public Shipment failDelivery(User shipper, Long shipmentId, String reason) {
        Shipment shipment = getShipmentForShipper(shipper, shipmentId);

        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new IllegalStateException("Không thể báo giao thất bại cho đơn hàng đã giao thành công!");
        }

        LocalDateTime now = LocalDateTime.now();
        shipment.setStatus(ShipmentStatus.DELIVERY_FAILED);
        shipment.setUpdatedAt(now);

        int currentAttempts = (shipment.getAttemptCount() != null ? shipment.getAttemptCount() : 0) + 1;
        shipment.setAttemptCount(currentAttempts);

        Order order = shipment.getOrder();
        String trimmedReason = (reason != null && !reason.isBlank()) ? reason.trim() : "Khách không nhận hàng";

        boolean isCustomerReturnOrCancel = false;
        String lowerReason = trimmedReason.toLowerCase();
        if (lowerReason.contains("hoàn") 
                || lowerReason.contains("hủy") 
                || lowerReason.contains("huy") 
                || lowerReason.contains("từ chối") 
                || lowerReason.contains("không nhận") 
                || lowerReason.contains("khong nhan") 
                || lowerReason.contains("đổi ý") 
                || lowerReason.contains("doi y")) {
            isCustomerReturnOrCancel = true;
        }

        String failLog;
        if (isCustomerReturnOrCancel) {
            order.setStatus(OrderStatus.CANCELLED);
            failLog = "[Shipper báo giao thất bại (Khách hủy/hoàn hàng): " + trimmedReason + " -> Đã hủy đơn và hoàn kho]";
        } else {
            if (currentAttempts < 3) {
                failLog = "[Shipper báo giao thất bại lần " + currentAttempts + "/3: " + trimmedReason + " - Chờ giao lại]";
            } else {
                order.setStatus(OrderStatus.CANCELLED);
                failLog = "[Shipper báo giao thất bại lần " + currentAttempts + "/3: " + trimmedReason + " -> Quá 3 lần giao thất bại, tự động chuyển hủy đơn và hoàn kho]";
            }
        }

        String currentNote = order.getNote();
        order.setNote(currentNote != null && !currentNote.isBlank() ? failLog + " | " + currentNote : failLog);
        order.setUpdatedAt(now);
        orderRepository.save(order);

        Shipment saved = shipmentRepository.save(shipment);
        log.warn("Shipper '{}' báo giao thất bại cho đơn '{}'. Lần: {}, Lý do: {}, Đơn hủy: {}", 
                shipper.getEmail(), order.getOrderCode(), currentAttempts, trimmedReason, order.getStatus() == OrderStatus.CANCELLED);
        return saved;
    }

    // ==========================================
    // 3. COMMON QUERY METHODS (TRUY VẤN DÙNG CHUNG)
    // ==========================================

    @Transactional(readOnly = true)
    public Order getOrderDetail(User user, Long orderId) {
        if (user.getRole() == Role.STAFF) {
            return getOrderForStaff(user, orderId);
        } else if (user.getRole() == Role.SHIPPER) {
            Shipment s = shipmentRepository.findByOrder_Id(orderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chuyến giao cho đơn #" + orderId));
            if (!s.getShipper().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập đơn của shipper khác!");
            }
            return s.getOrder();
        } else {
            return orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng #" + orderId));
        }
    }

    @Transactional(readOnly = true)
    public Shipment getShipmentDetail(User shipper, Long shipmentId) {
        return getShipmentForShipper(shipper, shipmentId);
    }

    @Transactional(readOnly = true)
    public Optional<Shipment> getOrderShipment(Long orderId) {
        return shipmentRepository.findByOrder_Id(orderId);
    }

    @Transactional(readOnly = true)
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findAllByOrder_IdOrderByIdAsc(orderId);
    }

    /**
     * Thuật toán phân công Shipper thông minh cho chi nhánh:
     * - Tiêu chí 1: Cân bằng tải - Chọn Shipper có ít đơn đang giao nhất (activeOrdersCount ASC).
     * - Tiêu chí 2 (Ưu tiên 1 khi số đơn bằng nhau): Người chờ lâu nhất nhận trước
     *   (Shipper chưa nhận đơn nào hoặc có thời điểm nhận đơn gần nhất xa nhất trong quá khứ).
     * - Tiêu chí 3: ID tăng dần (deterministic).
     */
    @Transactional(readOnly = true)
    public List<example.com.ltw.staff.dto.ShipperAssignmentOptionDto> getBranchShippers(Long branchId) {
        List<User> shippers = userRepository.findAllByBranch_IdAndRoleOrderByCreatedAtDesc(branchId, Role.SHIPPER);
        if (shippers.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now();
        List<example.com.ltw.staff.dto.ShipperAssignmentOptionDto> options = new java.util.ArrayList<>();

        for (User s : shippers) {
            long activeOrders = shipmentRepository.countActiveDeliveriesByShipper(s.getId());
            LocalDateTime lastAssigned = shipmentRepository.findLatestAssignmentTimeByShipper(s.getId());

            String waitDesc;
            if (lastAssigned == null) {
                waitDesc = "Chưa có đơn";
            } else {
                long minutes = java.time.Duration.between(lastAssigned, now).toMinutes();
                if (minutes < 1) {
                    waitDesc = "Vừa nhận đơn";
                } else if (minutes < 60) {
                    waitDesc = "Chờ " + minutes + " phút";
                } else {
                    long hours = minutes / 60;
                    long remMins = minutes % 60;
                    waitDesc = "Chờ " + hours + " giờ" + (remMins > 0 ? " " + remMins + " phút" : "");
                }
            }

            options.add(example.com.ltw.staff.dto.ShipperAssignmentOptionDto.builder()
                    .id(s.getId())
                    .fullName(s.getFullName())
                    .phone(s.getPhone() != null ? s.getPhone() : "Chưa có SĐT")
                    .email(s.getEmail())
                    .activeOrdersCount(activeOrders)
                    .lastAssignedAt(lastAssigned)
                    .waitingTimeDescription(waitDesc)
                    .recommended(false)
                    .build());
        }

        // Sắp xếp theo đúng ưu tiên:
        // 1. Ít việc nhất (activeOrdersCount ASC)
        // 2. Người chờ lâu nhất nhận trước (lastAssignedAt: nullsFirst -> naturalOrder ASC)
        // 3. ID ASC
        options.sort(
                java.util.Comparator.comparingLong(example.com.ltw.staff.dto.ShipperAssignmentOptionDto::getActiveOrdersCount)
                        .thenComparing(example.com.ltw.staff.dto.ShipperAssignmentOptionDto::getLastAssignedAt,
                                java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder()))
                        .thenComparing(example.com.ltw.staff.dto.ShipperAssignmentOptionDto::getId)
        );

        if (!options.isEmpty()) {
            options.get(0).setRecommended(true);
        }

        return options;
    }

    /**
     * Tự động tìm Shipper tối ưu nhất theo thuật toán cân bằng tải & người chờ lâu nhất nhận trước.
     */
    @Transactional(readOnly = true)
    public User findOptimalShipper(Long branchId) {
        List<example.com.ltw.staff.dto.ShipperAssignmentOptionDto> ranked = getBranchShippers(branchId);
        if (ranked.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chi nhánh hiện chưa có nhân viên Shipper nào đang hoạt động!");
        }

        Long bestShipperId = ranked.get(0).getId();
        return userRepository.findById(bestShipperId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin Shipper được đề xuất!"));
    }

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================

    private Order getOrderForStaff(User staff, Long orderId) {
        Branch branch = staff.getBranch();
        if (branch == null) {
            User current = userRepository.findById(staff.getId()).orElse(staff);
            branch = current.getBranch();
        }
        if (branch == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản nhân viên chưa được gán chi nhánh!");
        }

        return orderRepository.findByIdAndBranch_Id(orderId, branch.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Đơn hàng #" + orderId + " không tồn tại hoặc không thuộc chi nhánh của bạn!"
                ));
    }

    private Shipment getShipmentForShipper(User shipper, Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chuyến giao hàng #" + shipmentId));

        if (shipment.getShipper() == null || !shipment.getShipper().getId().equals(shipper.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập đơn hàng này!");
        }

        return shipment;
    }
}
