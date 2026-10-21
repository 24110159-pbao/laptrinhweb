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
     */
    @Transactional
    public Order confirmOrder(User staff, Long orderId) {
        Order order = getOrderForStaff(staff, orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Đơn hàng #" + order.getOrderCode() + " hiện ở trạng thái '" + order.getStatus() + "', không thể xác nhận lại!");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        log.info("Staff '{}' đã xác nhận đơn hàng '{}' tại chi nhánh '{}'",
                staff.getEmail(), order.getOrderCode(), order.getBranch().getName());
        return saved;
    }

    /**
     * Bước 3: Staff bàn giao đơn hàng cho Shipper cùng chi nhánh & phát hành mã GHN.
     */
    @Transactional
    public Shipment assignShipper(User staff, Long orderId, Long shipperId, boolean generateGhn) {
        Order order = getOrderForStaff(staff, orderId);

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Không thể bàn giao vận chuyển cho đơn hàng đã hủy hoặc đã giao thành công!");
        }

        Branch branch = order.getBranch();
        User shipper = userRepository.findByIdAndBranch_IdAndRole(shipperId, branch.getId(), Role.SHIPPER)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Shipper hợp lệ trong cùng chi nhánh '" + branch.getName() + "'!"
                ));

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

        // Cập nhật Order sang trạng thái chuẩn bị giao vận
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
        }
        order.setUpdatedAt(LocalDateTime.now());
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

        LocalDateTime now = LocalDateTime.now();
        shipment.setStatus(ShipmentStatus.RECEIVED);
        shipment.setUpdatedAt(now);

        Order order = shipment.getOrder();
        order.setStatus(OrderStatus.SHIPPING);
        order.setUpdatedAt(now);
        orderRepository.save(order);

        Shipment saved = shipmentRepository.save(shipment);
        log.info("Shipper '{}' kích hoạt giao lại cho đơn '{}'", shipper.getEmail(), order.getOrderCode());
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

        Order order = shipment.getOrder();
        String failLog = "[Shipper báo thất bại: " + (reason != null ? reason.trim() : "Khách không nhận") + "]";
        String currentNote = order.getNote();
        order.setNote(currentNote != null && !currentNote.isBlank() ? failLog + " | " + currentNote : failLog);
        order.setUpdatedAt(now);
        orderRepository.save(order);

        Shipment saved = shipmentRepository.save(shipment);
        log.warn("Shipper '{}' báo giao thất bại cho đơn '{}'. Lý do: {}", shipper.getEmail(), order.getOrderCode(), reason);
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

    @Transactional(readOnly = true)
    public List<User> getBranchShippers(Long branchId) {
        return userRepository.findAllByBranch_IdAndRoleOrderByCreatedAtDesc(branchId, Role.SHIPPER);
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
