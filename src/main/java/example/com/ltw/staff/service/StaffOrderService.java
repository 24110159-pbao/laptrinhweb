package example.com.ltw.staff.service;

import example.com.ltw.branch.entity.Branch;
import example.com.ltw.branch.repository.BranchRepository;
import example.com.ltw.order.entity.Order;
import example.com.ltw.order.entity.OrderItem;
import example.com.ltw.order.entity.OrderStatus;
import example.com.ltw.order.repository.OrderItemRepository;
import example.com.ltw.order.repository.OrderRepository;
import example.com.ltw.staff.dto.StaffDashboardDto;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
public class StaffOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final example.com.ltw.shipping.repository.ShipmentRepository shipmentRepository;
    private final example.com.ltw.shipping.service.GhnShippingService ghnShippingService;
    private final example.com.ltw.order.service.BranchOrderWorkflowService workflowService;

    public StaffOrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            BranchRepository branchRepository,
            UserRepository userRepository,
            example.com.ltw.shipping.repository.ShipmentRepository shipmentRepository,
            example.com.ltw.shipping.service.GhnShippingService ghnShippingService,
            example.com.ltw.order.service.BranchOrderWorkflowService workflowService
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.shipmentRepository = shipmentRepository;
        this.ghnShippingService = ghnShippingService;
        this.workflowService = workflowService;
    }

    /**
     * Resolve the branch for the staff.
     * If user.branch is null, fallback to the default active branch to prevent NPE.
     */
    @Transactional
    public Branch resolveStaffBranch(User staff) {
        if (staff == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập!");
        }

        User currentStaff = (staff.getId() != null) ? userRepository.findById(staff.getId()).orElse(staff) : staff;

        if (currentStaff.getBranch() != null) {
            staff.setBranch(currentStaff.getBranch());
            return currentStaff.getBranch();
        }

        Branch fallbackBranch = branchRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hệ thống chưa có chi nhánh nào được cấu hình!"));

        currentStaff.setBranch(fallbackBranch);
        userRepository.save(currentStaff);
        staff.setBranch(fallbackBranch);
        log.warn("Auto-assigned staff {} to default branch '{}'", currentStaff.getEmail(), fallbackBranch.getName());
        return fallbackBranch;
    }

    /**
     * Collect dashboard metrics exclusively for the staff's branch (default all time).
     */
    @Transactional(readOnly = true)
    public StaffDashboardDto getDashboardData(User staff) {
        return getDashboardData(staff, "all", null, null);
    }

    /**
     * Collect dashboard metrics with date-time filtering scoped strictly to the staff's branch.
     */
    @Transactional(readOnly = true)
    public StaffDashboardDto getDashboardData(User staff, String timePreset, java.time.LocalDateTime fromDate, java.time.LocalDateTime toDate) {
        Branch branch = resolveStaffBranch(staff);
        Long branchId = branch.getId();

        java.time.LocalDateTime from = fromDate;
        java.time.LocalDateTime to = toDate;
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        java.time.format.DateTimeFormatter df = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String formattedRange = "Toàn thời gian";

        if ("today".equalsIgnoreCase(timePreset)) {
            from = java.time.LocalDate.now().atStartOfDay();
            to = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);
            formattedRange = "Hôm nay (" + java.time.LocalDate.now().format(df) + ")";
        } else if ("yesterday".equalsIgnoreCase(timePreset)) {
            java.time.LocalDate yesterday = java.time.LocalDate.now().minusDays(1);
            from = yesterday.atStartOfDay();
            to = yesterday.atTime(java.time.LocalTime.MAX);
            formattedRange = "Hôm qua (" + yesterday.format(df) + ")";
        } else if ("last7days".equalsIgnoreCase(timePreset)) {
            from = java.time.LocalDateTime.now().minusDays(7);
            to = java.time.LocalDateTime.now();
            formattedRange = "7 ngày qua (Từ " + from.format(df) + " đến " + to.format(df) + ")";
        } else if ("thisMonth".equalsIgnoreCase(timePreset)) {
            from = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay();
            to = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);
            formattedRange = "Tháng " + java.time.LocalDate.now().getMonthValue() + "/" + java.time.LocalDate.now().getYear();
        } else if (from != null || to != null) {
            if (from != null && to != null) {
                formattedRange = "Từ " + from.format(dtf) + " đến " + to.format(dtf);
            } else if (from != null) {
                formattedRange = "Từ " + from.format(dtf);
            } else {
                formattedRange = "Đến " + to.format(dtf);
            }
        }

        long totalOrders = orderRepository.countByBranchAndTime(branchId, from, to);
        long pendingOrders = orderRepository.countByBranchAndStatusAndTime(branchId, OrderStatus.PENDING, from, to);
        long confirmedOrders = orderRepository.countByBranchAndStatusAndTime(branchId, OrderStatus.CONFIRMED, from, to);
        long shippingOrders = orderRepository.countByBranchAndStatusAndTime(branchId, OrderStatus.SHIPPING, from, to);
        long deliveredOrders = orderRepository.countByBranchAndStatusAndTime(branchId, OrderStatus.DELIVERED, from, to);
        long cancelledOrders = orderRepository.countByBranchAndStatusAndTime(branchId, OrderStatus.CANCELLED, from, to);

        java.math.BigDecimal totalOrderAmount = orderRepository.sumTotalOrderAmountByBranchAndTime(branchId, from, to);
        java.math.BigDecimal totalRevenue = orderRepository.sumTotalRevenueByBranchAndTime(branchId, from, to);
        java.math.BigDecimal collectedRevenue = orderRepository.sumPaidRevenueByBranchAndTime(branchId, from, to);
        java.math.BigDecimal pendingCodRevenue = orderRepository.sumPendingCodRevenueByBranchAndTime(branchId, from, to);
        long paidOrders = orderRepository.countByBranchAndPaymentStatusAndTime(branchId, example.com.ltw.order.entity.PaymentStatus.PAID, from, to);
        long unpaidOrders = orderRepository.countByBranchAndPaymentStatusAndTime(branchId, example.com.ltw.order.entity.PaymentStatus.UNPAID, from, to);
        long pendingCodOrders = orderRepository.countPendingCodOrdersByBranchAndTime(branchId, from, to);

        List<Order> recentOrders = orderRepository.findRecentOrdersByBranchAndTime(branchId, from, to, PageRequest.of(0, 5));
        List<Order> urgentOrders = orderRepository.findUrgentOrdersByBranchAndTime(branchId, OrderStatus.PENDING, from, to, PageRequest.of(0, 5));

        return StaffDashboardDto.builder()
                .branchId(branchId)
                .branchName(branch.getName())
                .branchAddress(branch.getAddress())
                .branchPhone(branch.getPhone() != null ? branch.getPhone() : "Chưa cập nhật")
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .confirmedOrders(confirmedOrders)
                .shippingOrders(shippingOrders)
                .deliveredOrders(deliveredOrders)
                .cancelledOrders(cancelledOrders)
                .totalOrderAmount(totalOrderAmount)
                .totalRevenue(totalRevenue)
                .collectedRevenue(collectedRevenue)
                .pendingCodRevenue(pendingCodRevenue)
                .paidOrders(paidOrders)
                .unpaidOrders(unpaidOrders)
                .pendingCodOrders(pendingCodOrders)
                .recentOrders(recentOrders)
                .urgentOrders(urgentOrders)
                .timePreset(timePreset != null ? timePreset : "all")
                .fromDateTime(from != null ? from.toString() : "")
                .toDateTime(to != null ? to.toString() : "")
                .formattedTimeRange(formattedRange)
                .build();
    }

    /**
     * Get paginated orders scoped strictly by staff branch (default all time).
     */
    @Transactional(readOnly = true)
    public Page<Order> getOrders(User staff, OrderStatus status, String keyword, int page, int size) {
        return getOrders(staff, status, keyword, null, null, page, size);
    }

    /**
     * Get paginated orders scoped strictly by staff branch and filtered by date-time range.
     */
    @Transactional(readOnly = true)
    public Page<Order> getOrders(User staff, OrderStatus status, String keyword, java.time.LocalDateTime fromDate, java.time.LocalDateTime toDate, int page, int size) {
        Branch branch = resolveStaffBranch(staff);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));

        String sanitizedKeyword = (keyword != null) ? keyword.trim() : "";
        return orderRepository.searchOrdersByBranchAndTime(branch.getId(), status, sanitizedKeyword, fromDate, toDate, pageable);
    }

    /**
     * Retrieve order details with strict branch boundary enforcement.
     */
    @Transactional(readOnly = true)
    public Order getOrderDetail(User staff, Long orderId) {
        return workflowService.getOrderDetail(staff, orderId);
    }

    /**
     * Retrieve order item line entries for a given order.
     */
    @Transactional(readOnly = true)
    public List<OrderItem> getOrderItems(Long orderId) {
        return workflowService.getOrderItems(orderId);
    }

    /**
     * Confirm order (PENDING -> CONFIRMED) within staff branch.
     */
    @Transactional
    public Order confirmOrder(User staff, Long orderId) {
        return workflowService.confirmOrder(staff, orderId);
    }

    /**
     * Cancel order with specified reason (Only allowed for PENDING or CONFIRMED orders).
     */
    @Transactional
    public Order cancelOrder(User staff, Long orderId, String reason) {
        return workflowService.cancelOrder(staff, orderId, reason);
    }

    /**
     * Get all active shippers attached to the staff's branch.
     */
    @Transactional(readOnly = true)
    public List<User> getBranchShippers(Long branchId) {
        return workflowService.getBranchShippers(branchId);
    }

    /**
     * Retrieve shipment record for a given order if exists.
     */
    @Transactional(readOnly = true)
    public java.util.Optional<example.com.ltw.shipping.entity.Shipment> getOrderShipment(Long orderId) {
        return workflowService.getOrderShipment(orderId);
    }

    /**
     * Assign shipper to order & issue GHN order code (Giao Hàng Nhanh tracking).
     */
    @Transactional
    public example.com.ltw.shipping.entity.Shipment assignShipper(User staff, Long orderId, Long shipperId, boolean generateGhn) {
        return workflowService.assignShipper(staff, orderId, shipperId, generateGhn);
    }
}
