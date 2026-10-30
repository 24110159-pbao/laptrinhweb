package example.com.ltw.shipper.service;

import example.com.ltw.order.entity.Order;
import example.com.ltw.order.entity.OrderStatus;
import example.com.ltw.order.entity.PaymentMethod;
import example.com.ltw.order.entity.PaymentStatus;
import example.com.ltw.order.repository.OrderRepository;
import example.com.ltw.shipper.dto.ShipperDashboardDto;
import example.com.ltw.shipping.entity.Shipment;
import example.com.ltw.shipping.entity.ShipmentStatus;
import example.com.ltw.shipping.repository.ShipmentRepository;
import example.com.ltw.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ShipperOrderService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final example.com.ltw.user.repository.UserRepository userRepository;
    private final example.com.ltw.branch.repository.BranchRepository branchRepository;
    private final example.com.ltw.order.repository.OrderItemRepository orderItemRepository;
    private final example.com.ltw.order.service.BranchOrderWorkflowService workflowService;

    public ShipperOrderService(
            ShipmentRepository shipmentRepository,
            OrderRepository orderRepository,
            example.com.ltw.user.repository.UserRepository userRepository,
            example.com.ltw.branch.repository.BranchRepository branchRepository,
            example.com.ltw.order.repository.OrderItemRepository orderItemRepository,
            example.com.ltw.order.service.BranchOrderWorkflowService workflowService
    ) {
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.orderItemRepository = orderItemRepository;
        this.workflowService = workflowService;
    }

    /**
     * Resolve the branch for the shipper.
     */
    @Transactional
    public example.com.ltw.branch.entity.Branch resolveShipperBranch(User shipper) {
        if (shipper == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập!");
        }
        User current = userRepository.findById(shipper.getId()).orElse(shipper);
        if (current.getBranch() != null && current.getBranch().getId() != null) {
            return branchRepository.findById(current.getBranch().getId()).orElse(current.getBranch());
        }
        return branchRepository.findAll().stream().findFirst().orElseGet(() -> {
            example.com.ltw.branch.entity.Branch b = new example.com.ltw.branch.entity.Branch();
            b.setName("NexusTech Flagship Store");
            b.setAddress("Hệ thống Flagship Toàn quốc");
            b.setPhone("1800 6868");
            b.setEmail("support@nexustech.vn");
            return b;
        });
    }

    /**
     * Get dashboard metrics strictly for the authenticated shipper (default all time).
     */
    @Transactional(readOnly = true)
    public ShipperDashboardDto getDashboardData(User shipper) {
        return getDashboardData(shipper, "all", null, null);
    }

    /**
     * Get dashboard metrics with date-time filtering scoped strictly to the authenticated shipper.
     */
    @Transactional(readOnly = true)
    public ShipperDashboardDto getDashboardData(User shipper, String timePreset, LocalDateTime fromDate, LocalDateTime toDate) {
        Long shipperId = shipper.getId();
        example.com.ltw.branch.entity.Branch branch = resolveShipperBranch(shipper);

        LocalDateTime from = fromDate;
        LocalDateTime to = toDate;
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        java.time.format.DateTimeFormatter df = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String formattedRange = "Toàn thời gian";

        if ("yesterday".equalsIgnoreCase(timePreset)) {
            java.time.LocalDate yesterday = java.time.LocalDate.now().minusDays(1);
            from = yesterday.atStartOfDay();
            to = yesterday.atTime(java.time.LocalTime.MAX);
            formattedRange = "Hôm qua (" + yesterday.format(df) + ")";
        } else if ("thisWeek".equalsIgnoreCase(timePreset)) {
            java.time.LocalDate now = java.time.LocalDate.now();
            java.time.LocalDate startOfWeek = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            from = startOfWeek.atStartOfDay();
            to = now.atTime(java.time.LocalTime.MAX);
            formattedRange = "Tuần này (Từ " + startOfWeek.format(df) + " đến " + now.format(df) + ")";
        } else if ("thisMonth".equalsIgnoreCase(timePreset)) {
            from = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay();
            to = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);
            formattedRange = "Tháng " + java.time.LocalDate.now().getMonthValue() + "/" + java.time.LocalDate.now().getYear();
        } else if ("all".equalsIgnoreCase(timePreset)) {
            from = null;
            to = null;
            formattedRange = "Toàn thời gian";
        } else {
            // Default: today (Hôm nay)
            from = java.time.LocalDate.now().atStartOfDay();
            to = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);
            formattedRange = "Hôm nay (" + java.time.LocalDate.now().format(df) + ")";
        }

        long pendingPickups = shipmentRepository.countActiveByShipperAndStatusAndTime(shipperId, ShipmentStatus.PENDING, from, to);
        long activeDeliveries = shipmentRepository.countActiveByShipperAndStatusAndTime(shipperId, ShipmentStatus.RECEIVED, from, to);
        long deliveredCount = shipmentRepository.countDeliveredByShipperAndTime(shipperId, from, to);
        long failedCount = shipmentRepository.countFailedByShipperAndTime(shipperId, from, to);
        long totalAssigned = shipmentRepository.countByShipperAndTime(shipperId, from, to);

        java.math.BigDecimal totalCodToCollect = shipmentRepository.sumCodToCollectByShipperAndTime(shipperId, from, to);
        java.math.BigDecimal totalCodCollected = shipmentRepository.sumCodCollectedByShipperAndTime(shipperId, from, to);
        long paidOnlineCount = shipmentRepository.countPaidOnlineByShipperAndTime(shipperId, from, to);

        List<Shipment> recentShipments = shipmentRepository.findRecentShipmentsByShipperAndTime(shipperId, from, to, PageRequest.of(0, 5));
        List<Shipment> urgentShipments = shipmentRepository.findUrgentShipmentsByShipperAndTime(shipperId, from, to, PageRequest.of(0, 5));

        return ShipperDashboardDto.builder()
                .shipperId(shipperId)
                .shipperName(shipper.getFullName())
                .shipperPhone(shipper.getPhone() != null ? shipper.getPhone() : "Chưa cập nhật")
                .branchId(branch.getId())
                .branchName(branch.getName())
                .branchAddress(branch.getAddress())
                .branchPhone(branch.getPhone() != null ? branch.getPhone() : "1800 6868")
                .branchEmail(branch.getEmail() != null ? branch.getEmail() : "support@nexustech.vn")
                .pendingPickups(pendingPickups)
                .activeDeliveries(activeDeliveries)
                .deliveredCount(deliveredCount)
                .failedCount(failedCount)
                .totalAssigned(totalAssigned)
                .totalCodToCollect(totalCodToCollect)
                .totalCodCollected(totalCodCollected)
                .paidOnlineCount(paidOnlineCount)
                .recentShipments(recentShipments)
                .urgentShipments(urgentShipments)
                .timePreset(timePreset != null ? timePreset : "all")
                .fromDateTime(from != null ? from.toString() : "")
                .toDateTime(to != null ? to.toString() : "")
                .formattedTimeRange(formattedRange)
                .build();
    }

    /**
     * Get paginated delivery tasks assigned to this shipper (default all time).
     */
    @Transactional(readOnly = true)
    public Page<Shipment> getShipperTasks(User shipper, ShipmentStatus status, int page, int size) {
        return getShipperTasks(shipper, status, null, null, null, page, size);
    }

    /**
     * Get paginated delivery tasks assigned to this shipper with search & date-time range.
     */
    @Transactional(readOnly = true)
    public Page<Shipment> getShipperTasks(User shipper, ShipmentStatus status, String keyword, LocalDateTime fromDate, LocalDateTime toDate, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        String sanitizedKeyword = (keyword != null) ? keyword.trim() : "";
        Long shipperId = shipper.getId();

        if (status == ShipmentStatus.PENDING || status == ShipmentStatus.RECEIVED) {
            return shipmentRepository.searchActiveShipperTasksByStatus(shipperId, status, sanitizedKeyword, fromDate, toDate, pageable);
        } else if (status == ShipmentStatus.DELIVERED) {
            return shipmentRepository.searchDeliveredShipperTasks(shipperId, sanitizedKeyword, fromDate, toDate, pageable);
        } else if (status == ShipmentStatus.DELIVERY_FAILED || status == ShipmentStatus.CANCELLED) {
            return shipmentRepository.searchFailedShipperTasks(shipperId, sanitizedKeyword, fromDate, toDate, pageable);
        } else {
            return shipmentRepository.searchAllShipperTasks(shipperId, sanitizedKeyword, fromDate, toDate, pageable);
        }
    }

    /**
     * Get shipment details with strict Shipper Boundary Enforcement (delegates to BranchOrderWorkflowService).
     */
    @Transactional(readOnly = true)
    public Shipment getShipmentDetail(User shipper, Long shipmentId) {
        return workflowService.getShipmentDetail(shipper, shipmentId);
    }

    /**
     * Action 1: Shipper accepts and receives package from store branch (PENDING -> RECEIVED).
     * Delegates to BranchOrderWorkflowService for unified lifecycle synchronization.
     */
    @Transactional
    public Shipment receiveShipment(User shipper, Long shipmentId) {
        return workflowService.receivePackage(shipper, shipmentId);
    }

    /**
     * Action 2: Shipper confirms successful delivery (RECEIVED -> DELIVERED).
     * If PaymentMethod == COD, automatically update order payment status to PAID!
     * Delegates to BranchOrderWorkflowService.
     */
    @Transactional
    public Shipment completeDelivery(User shipper, Long shipmentId) {
        return workflowService.completeDelivery(shipper, shipmentId);
    }

    /**
     * Action 3: Shipper reports delivery failure with specific reason.
     * Delegates to BranchOrderWorkflowService.
     */
    @Transactional
    public Shipment failDelivery(User shipper, Long shipmentId, String reason) {
        return workflowService.failDelivery(shipper, shipmentId, reason);
    }

    /**
     * Action 4: Shipper retries delivery for failed task (DELIVERY_FAILED -> RECEIVED).
     */
    @Transactional
    public Shipment retryDelivery(User shipper, Long shipmentId) {
        return workflowService.retryDelivery(shipper, shipmentId);
    }

    /**
     * Action 5: Universal status updater for Shipper tasks.
     */
    @Transactional
    public Shipment updateStatus(User shipper, Long shipmentId, ShipmentStatus newStatus, String reason) {
        return workflowService.updateShipmentStatus(shipper, shipmentId, newStatus, reason);
    }

    /**
     * Get items belonging to an order.
     * Delegates to BranchOrderWorkflowService.
     */
    @Transactional(readOnly = true)
    public List<example.com.ltw.order.entity.OrderItem> getOrderItems(Long orderId) {
        return workflowService.getOrderItems(orderId);
    }
}
