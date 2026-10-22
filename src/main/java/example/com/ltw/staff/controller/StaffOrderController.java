package example.com.ltw.staff.controller;

import example.com.ltw.auth.service.AuthService;
import example.com.ltw.order.entity.Order;
import example.com.ltw.order.entity.OrderItem;
import example.com.ltw.order.entity.OrderStatus;
import example.com.ltw.staff.dto.StaffDashboardDto;
import example.com.ltw.staff.service.StaffOrderService;
import example.com.ltw.user.entity.Role;
import example.com.ltw.user.entity.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/staff")
public class StaffOrderController {

    private final StaffOrderService staffOrderService;

    public StaffOrderController(StaffOrderService staffOrderService) {
        this.staffOrderService = staffOrderService;
    }

    /**
     * Authenticate and authorize current user as Staff / Branch Manager / Admin.
     */
    private User requireStaffUser(HttpSession session) {
        User user = (User) session.getAttribute(AuthService.SESSION_CURRENT_USER);
        if (user == null) {
            throw new SecurityException("UNAUTHENTICATED");
        }
        if (user.getRole() != Role.STAFF && user.getRole() != Role.MANAGER && user.getRole() != Role.ADMIN) {
            throw new SecurityException("FORBIDDEN");
        }
        return user;
    }

    private java.time.LocalDateTime parseDateTime(String text, boolean isEnd) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String s = text.trim();
        try {
            if (s.length() == 10) { // yyyy-MM-dd
                java.time.LocalDate date = java.time.LocalDate.parse(s);
                return isEnd ? date.atTime(java.time.LocalTime.MAX) : date.atStartOfDay();
            }
            if (s.length() == 16) { // yyyy-MM-ddTHH:mm
                return java.time.LocalDateTime.parse(s);
            }
            return java.time.LocalDateTime.parse(s);
        } catch (Exception ex) {
            log.warn("Could not parse date-time string: {}", text);
            return null;
        }
    }

    // ==========================================
    // 1. STAFF DASHBOARD
    // ==========================================

    @GetMapping({"", "/dashboard"})
    public String showDashboard(
            @RequestParam(name = "scope", defaultValue = "all") String scope,
            @RequestParam(name = "timePreset", defaultValue = "all") String timePreset,
            @RequestParam(name = "fromDateTime", required = false) String fromDateTimeStr,
            @RequestParam(name = "toDateTime", required = false) String toDateTimeStr,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User staff;
        try {
            staff = requireStaffUser(session);
        } catch (SecurityException ex) {
            if ("UNAUTHENTICATED".equals(ex.getMessage())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập tài khoản nhân viên để tiếp tục!");
                return "redirect:/login";
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập vào cổng quản lý nhân viên!");
            return "redirect:/";
        }

        java.time.LocalDateTime fromDate = parseDateTime(fromDateTimeStr, false);
        java.time.LocalDateTime toDate = parseDateTime(toDateTimeStr, true);

        StaffDashboardDto dashboard = staffOrderService.getDashboardData(staff, timePreset, fromDate, toDate, scope);
        List<example.com.ltw.staff.dto.ShipperAssignmentOptionDto> branchShippers = staffOrderService.getBranchShippers(dashboard.getBranchId());

        model.addAttribute("staff", staff);
        model.addAttribute("currentUser", staff);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("branchShippers", branchShippers);
        model.addAttribute("locationName", dashboard.getBranchName());
        model.addAttribute("cartCount", 0);
        model.addAttribute("compareCount", 0);
        model.addAttribute("activeNav", "dashboard");
        model.addAttribute("scope", scope);
        model.addAttribute("selectedTimePreset", timePreset);
        model.addAttribute("fromDateTime", fromDateTimeStr != null ? fromDateTimeStr : "");
        model.addAttribute("toDateTime", toDateTimeStr != null ? toDateTimeStr : "");
        return "staff/dashboard";
    }

    public String showDashboard(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        return showDashboard("all", "all", null, null, model, session, redirectAttributes);
    }

    // ==========================================
    // 2. MY MANAGED ORDERS (ĐƠN QUẢN LÝ CỦA NHÂN VIÊN)
    // ==========================================

    @GetMapping("/orders")
    public String listOrders(
            @RequestParam(name = "status", required = false) OrderStatus status,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "timePreset", defaultValue = "all") String timePreset,
            @RequestParam(name = "fromDateTime", required = false) String fromDateTimeStr,
            @RequestParam(name = "toDateTime", required = false) String toDateTimeStr,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User staff;
        try {
            staff = requireStaffUser(session);
        } catch (SecurityException ex) {
            if ("UNAUTHENTICATED".equals(ex.getMessage())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập tài khoản nhân viên!");
                return "redirect:/login";
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập vào cổng nhân viên!");
            return "redirect:/";
        }

        java.time.LocalDateTime fromDate = parseDateTime(fromDateTimeStr, false);
        java.time.LocalDateTime toDate = parseDateTime(toDateTimeStr, true);

        if ("today".equalsIgnoreCase(timePreset)) {
            fromDate = java.time.LocalDate.now().atStartOfDay();
            toDate = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);
        } else if ("yesterday".equalsIgnoreCase(timePreset)) {
            java.time.LocalDate yesterday = java.time.LocalDate.now().minusDays(1);
            fromDate = yesterday.atStartOfDay();
            toDate = yesterday.atTime(java.time.LocalTime.MAX);
        } else if ("last7days".equalsIgnoreCase(timePreset)) {
            fromDate = java.time.LocalDateTime.now().minusDays(7);
            toDate = java.time.LocalDateTime.now();
        } else if ("thisMonth".equalsIgnoreCase(timePreset)) {
            fromDate = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay();
            toDate = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);
        }

        // Đơn Quản Lý: Chỉ lấy các đơn do chính nhân viên này tiếp nhận & xử lý
        Page<Order> orderPage = staffOrderService.getMyManagedOrders(staff, status, keyword, fromDate, toDate, page, size);
        StaffDashboardDto dashboard = staffOrderService.getDashboardData(staff, timePreset, fromDate, toDate);
        List<example.com.ltw.staff.dto.ShipperAssignmentOptionDto> branchShippers = staffOrderService.getBranchShippers(dashboard.getBranchId());

        model.addAttribute("staff", staff);
        model.addAttribute("currentUser", staff);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("branchShippers", branchShippers);
        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("page", orderPage);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("locationName", dashboard.getBranchName());
        model.addAttribute("cartCount", 0);
        model.addAttribute("compareCount", 0);
        model.addAttribute("activeNav", "my-orders");
        model.addAttribute("selectedTimePreset", timePreset);
        model.addAttribute("fromDateTime", fromDateTimeStr != null ? fromDateTimeStr : "");
        model.addAttribute("toDateTime", toDateTimeStr != null ? toDateTimeStr : "");

        return "staff/orders";
    }

    // ==========================================
    // 2B. PENDING POOL ORDERS (ĐƠN CHỜ NHẬN CHI NHÁNH)
    // ==========================================

    @GetMapping("/pending-orders")
    public String listPendingOrders(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "timePreset", defaultValue = "all") String timePreset,
            @RequestParam(name = "fromDateTime", required = false) String fromDateTimeStr,
            @RequestParam(name = "toDateTime", required = false) String toDateTimeStr,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User staff;
        try {
            staff = requireStaffUser(session);
        } catch (SecurityException ex) {
            if ("UNAUTHENTICATED".equals(ex.getMessage())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập tài khoản nhân viên!");
                return "redirect:/login";
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập vào cổng nhân viên!");
            return "redirect:/";
        }

        java.time.LocalDateTime fromDate = parseDateTime(fromDateTimeStr, false);
        java.time.LocalDateTime toDate = parseDateTime(toDateTimeStr, true);

        if ("today".equalsIgnoreCase(timePreset)) {
            fromDate = java.time.LocalDate.now().atStartOfDay();
            toDate = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);
        } else if ("yesterday".equalsIgnoreCase(timePreset)) {
            java.time.LocalDate yesterday = java.time.LocalDate.now().minusDays(1);
            fromDate = yesterday.atStartOfDay();
            toDate = yesterday.atTime(java.time.LocalTime.MAX);
        } else if ("last7days".equalsIgnoreCase(timePreset)) {
            fromDate = java.time.LocalDateTime.now().minusDays(7);
            toDate = java.time.LocalDateTime.now();
        } else if ("thisMonth".equalsIgnoreCase(timePreset)) {
            fromDate = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay();
            toDate = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);
        }

        Page<Order> orderPage = staffOrderService.getPendingPoolOrders(staff, keyword, fromDate, toDate, page, size);
        StaffDashboardDto dashboard = staffOrderService.getDashboardData(staff, timePreset, fromDate, toDate);
        List<example.com.ltw.staff.dto.ShipperAssignmentOptionDto> branchShippers = staffOrderService.getBranchShippers(dashboard.getBranchId());

        model.addAttribute("staff", staff);
        model.addAttribute("currentUser", staff);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("branchShippers", branchShippers);
        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("page", orderPage);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("locationName", dashboard.getBranchName());
        model.addAttribute("cartCount", 0);
        model.addAttribute("compareCount", 0);
        model.addAttribute("activeNav", "pending-orders");
        model.addAttribute("selectedTimePreset", timePreset);
        model.addAttribute("fromDateTime", fromDateTimeStr != null ? fromDateTimeStr : "");
        model.addAttribute("toDateTime", toDateTimeStr != null ? toDateTimeStr : "");

        return "staff/pending-orders";
    }

    // ==========================================
    // 3. ORDER DETAIL (CHỈ XEM KHI ĐÃ ĐƯỢC TIẾP NHẬN)
    // ==========================================

    @GetMapping("/orders/{id}")
    public String showOrderDetail(
            @PathVariable("id") Long id,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User staff;
        try {
            staff = requireStaffUser(session);
        } catch (SecurityException ex) {
            if ("UNAUTHENTICATED".equals(ex.getMessage())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập!");
                return "redirect:/login";
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập!");
            return "redirect:/";
        }

        try {
            Order order = staffOrderService.getOrderDetail(staff, id);

            // Quy tắc nghiệp vụ: Đơn hàng ở trạng thái PENDING chưa được tiếp nhận không thể xem chi tiết trực tiếp.
            // Phải tiếp nhận đơn trước để đưa vào quản lý, sau đó mới đóng gói và phân công Shipper.
            if (order.getStatus() == OrderStatus.PENDING) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Đơn hàng #" + order.getOrderCode() + " đang trong bể chờ nhận chi nhánh. Vui lòng bấm 'Tiếp nhận' để nhận vào Đơn Quản Lý trước khi xem chi tiết!");
                return "redirect:/staff/pending-orders";
            }

            List<OrderItem> items = staffOrderService.getOrderItems(id);
            List<example.com.ltw.staff.dto.ShipperAssignmentOptionDto> branchShippers = staffOrderService.getBranchShippers(order.getBranch().getId());
            java.util.Optional<example.com.ltw.shipping.entity.Shipment> shipment = staffOrderService.getOrderShipment(id);

            StaffDashboardDto dashboard = staffOrderService.getDashboardData(staff);

            model.addAttribute("staff", staff);
            model.addAttribute("currentUser", staff);
            model.addAttribute("dashboard", dashboard);
            model.addAttribute("order", order);
            model.addAttribute("orderItems", items);
            model.addAttribute("branchShippers", branchShippers);
            model.addAttribute("shipment", shipment.orElse(null));
            model.addAttribute("locationName", order.getBranch().getName());
            model.addAttribute("cartCount", 0);
            model.addAttribute("compareCount", 0);
            model.addAttribute("activeNav", "my-orders");
            return "staff/order-detail";
        } catch (Exception ex) {
            log.warn("Staff {} attempted to view unauthorized order {}: {}", staff.getEmail(), id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/staff/orders";
        }
    }

    // ==========================================
    // 4. CONFIRM ORDER (PENDING -> CONFIRMED)
    // ==========================================

    @PostMapping("/orders/{id}/confirm")
    public String confirmOrder(
            @PathVariable("id") Long id,
            @RequestParam(name = "redirect", required = false) String redirect,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User staff;
        try {
            staff = requireStaffUser(session);
        } catch (SecurityException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên làm việc hết hạn!");
            return "redirect:/login";
        }

        try {
            Order updated = staffOrderService.confirmOrder(staff, id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã tiếp nhận đơn hàng #" + updated.getOrderCode() + " thành công vào Đơn Quản Lý! Bạn có thể chọn đóng gói và phân công khi sẵn sàng.");
        } catch (IllegalStateException | org.springframework.web.server.ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            if ("pending".equalsIgnoreCase(redirect)) {
                return "redirect:/staff/pending-orders";
            }
            if ("dashboard".equalsIgnoreCase(redirect)) {
                return "redirect:/staff/dashboard";
            }
            return "redirect:/staff/orders";
        } catch (Exception ex) {
            log.error("Error confirming order {}", id, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tiếp nhận đơn hàng: " + ex.getMessage());
            return "redirect:/staff/pending-orders";
        }

        // Chuyển hướng về trang Đơn Quản Lý (/staff/orders)
        return "redirect:/staff/orders";
    }

    // ==========================================
    // 5. CANCEL ORDER WITH REASON
    // ==========================================

    @org.springframework.web.bind.annotation.PostMapping("/orders/{id}/cancel")
    public String cancelOrder(
            @PathVariable("id") Long id,
            @RequestParam(name = "reason", required = false, defaultValue = "Khách hàng yêu cầu hủy") String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User staff;
        try {
            staff = requireStaffUser(session);
        } catch (SecurityException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên làm việc hết hạn!");
            return "redirect:/login";
        }

        try {
            Order updated = staffOrderService.cancelOrder(staff, id, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã hủy đơn hàng #" + updated.getOrderCode() + " thành công!");
        } catch (IllegalStateException | org.springframework.web.server.ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Error cancelling order {}", id, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể hủy đơn hàng: " + ex.getMessage());
        }

        return "redirect:/staff/orders/" + id;
    }

    // ==========================================
    // 6. INVOICE VIEW & PDF PRINT
    // ==========================================

    @GetMapping("/orders/{id}/invoice")
    public String showInvoice(
            @PathVariable("id") Long id,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User staff;
        try {
            staff = requireStaffUser(session);
        } catch (SecurityException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập!");
            return "redirect:/login";
        }

        try {
            Order order = staffOrderService.getOrderDetail(staff, id);
            List<OrderItem> items = staffOrderService.getOrderItems(id);
            String amountInWords = example.com.ltw.staff.util.VietnameseCurrencyUtil.convertToWords(order.getFinalAmount());

            model.addAttribute("staff", staff);
            model.addAttribute("order", order);
            model.addAttribute("orderItems", items);
            model.addAttribute("amountInWords", amountInWords);
            return "staff/invoice";
        } catch (Exception ex) {
            log.warn("Staff {} attempted to view unauthorized invoice for order {}: {}", staff.getEmail(), id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/staff/orders";
        }
    }

    // ==========================================
    // 7. ASSIGN SHIPPER & ISSUE GHN TRACKING
    // ==========================================

    @PostMapping("/orders/{id}/assign-shipper")
    public String assignShipper(
            @PathVariable("id") Long id,
            @RequestParam(name = "shipperId", required = false) Long shipperId,
            @RequestParam(name = "generateGhn", defaultValue = "true") boolean generateGhn,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User staff;
        try {
            staff = requireStaffUser(session);
        } catch (SecurityException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập!");
            return "redirect:/login";
        }

        try {
            boolean isAutoAssigned = (shipperId == null || shipperId <= 0);
            example.com.ltw.shipping.entity.Shipment shipment = staffOrderService.assignShipper(staff, id, shipperId, generateGhn);
            String prefix = isAutoAssigned ? "🤖 [Tự động cân bằng tải] " : "";
            redirectAttributes.addFlashAttribute("successMessage",
                    prefix + "Đã bàn giao đơn hàng cho Shipper '" + shipment.getShipper().getFullName() +
                    "' và phát hành mã vận đơn GHN: " + shipment.getGhnOrderCode() + " thành công!");
        } catch (Exception ex) {
            log.error("Failed to assign shipper for order {}", id, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể phân công Shipper: " + ex.getMessage());
        }

        return "redirect:/staff/orders/" + id;
    }
}
