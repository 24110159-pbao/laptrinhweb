package example.com.ltw.shipper.controller;

import example.com.ltw.auth.service.AuthService;
import example.com.ltw.shipper.dto.ShipperDashboardDto;
import example.com.ltw.shipper.service.ShipperOrderService;
import example.com.ltw.shipping.entity.Shipment;
import example.com.ltw.shipping.entity.ShipmentStatus;
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

@Slf4j
@Controller
@RequestMapping("/shipper")
public class ShipperController {

    private final ShipperOrderService shipperOrderService;

    public ShipperController(ShipperOrderService shipperOrderService) {
        this.shipperOrderService = shipperOrderService;
    }

    /**
     * Authorize user as Shipper or Admin.
     */
    private User requireShipperUser(HttpSession session) {
        User user = (User) session.getAttribute(AuthService.SESSION_CURRENT_USER);
        if (user == null) {
            throw new SecurityException("UNAUTHENTICATED");
        }
        if (user.getRole() != Role.SHIPPER && user.getRole() != Role.ADMIN) {
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
    // 1. SHIPPER DASHBOARD
    // ==========================================

    @GetMapping({"", "/dashboard"})
    public String showDashboard(
            @RequestParam(name = "timePreset", defaultValue = "all") String timePreset,
            @RequestParam(name = "fromDateTime", required = false) String fromDateTimeStr,
            @RequestParam(name = "toDateTime", required = false) String toDateTimeStr,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User shipper;
        try {
            shipper = requireShipperUser(session);
        } catch (SecurityException ex) {
            if ("UNAUTHENTICATED".equals(ex.getMessage())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập tài khoản Shipper!");
                return "redirect:/login";
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập cổng giao nhận Shipper!");
            return "redirect:/";
        }

        java.time.LocalDateTime fromDate = null;
        java.time.LocalDateTime toDate = null;

        if ("yesterday".equalsIgnoreCase(timePreset)) {
            java.time.LocalDate yesterday = java.time.LocalDate.now().minusDays(1);
            fromDate = yesterday.atStartOfDay();
            toDate = yesterday.atTime(java.time.LocalTime.MAX);
        } else if ("thisWeek".equalsIgnoreCase(timePreset)) {
            java.time.LocalDate now = java.time.LocalDate.now();
            java.time.LocalDate startOfWeek = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            fromDate = startOfWeek.atStartOfDay();
            toDate = now.atTime(java.time.LocalTime.MAX);
        } else if ("thisMonth".equalsIgnoreCase(timePreset)) {
            fromDate = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay();
            toDate = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);
        } else if ("today".equalsIgnoreCase(timePreset)) {
            fromDate = java.time.LocalDate.now().atStartOfDay();
            toDate = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);
        } else {
            timePreset = "all";
            fromDate = null;
            toDate = null;
        }

        ShipperDashboardDto dashboard = shipperOrderService.getDashboardData(shipper, timePreset, fromDate, toDate);
        model.addAttribute("shipper", shipper);
        model.addAttribute("currentUser", shipper);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("locationName", dashboard.getBranchName());
        model.addAttribute("activeNav", "dashboard");
        model.addAttribute("selectedTimePreset", timePreset);
        model.addAttribute("fromDateTime", fromDateTimeStr != null ? fromDateTimeStr : "");
        model.addAttribute("toDateTime", toDateTimeStr != null ? toDateTimeStr : "");
        return "shipper/dashboard";
    }

    // ==========================================
    // 2. ASSIGNED DELIVERY TASKS
    // ==========================================

    @GetMapping("/tasks")
    public String listTasks(
            @RequestParam(name = "status", required = false) ShipmentStatus status,
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
        User shipper;
        try {
            shipper = requireShipperUser(session);
        } catch (SecurityException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập tài khoản Shipper!");
            return "redirect:/login";
        }

        java.time.LocalDateTime fromDate = null;
        java.time.LocalDateTime toDate = null;

        if ("yesterday".equalsIgnoreCase(timePreset)) {
            java.time.LocalDate yesterday = java.time.LocalDate.now().minusDays(1);
            fromDate = yesterday.atStartOfDay();
            toDate = yesterday.atTime(java.time.LocalTime.MAX);
        } else if ("thisWeek".equalsIgnoreCase(timePreset)) {
            java.time.LocalDate now = java.time.LocalDate.now();
            java.time.LocalDate startOfWeek = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            fromDate = startOfWeek.atStartOfDay();
            toDate = now.atTime(java.time.LocalTime.MAX);
        } else if ("thisMonth".equalsIgnoreCase(timePreset)) {
            fromDate = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay();
            toDate = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);
        } else if ("today".equalsIgnoreCase(timePreset)) {
            fromDate = java.time.LocalDate.now().atStartOfDay();
            toDate = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);
        } else {
            timePreset = "all";
            fromDate = null;
            toDate = null;
        }

        Page<Shipment> taskPage = shipperOrderService.getShipperTasks(shipper, status, keyword, fromDate, toDate, page, size);
        ShipperDashboardDto dashboard = shipperOrderService.getDashboardData(shipper, timePreset, fromDate, toDate);

        model.addAttribute("shipper", shipper);
        model.addAttribute("currentUser", shipper);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("tasks", taskPage.getContent());
        model.addAttribute("page", taskPage);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("shipmentStatuses", ShipmentStatus.values());
        model.addAttribute("locationName", dashboard.getBranchName());
        model.addAttribute("activeNav", "tasks");
        model.addAttribute("selectedTimePreset", timePreset);
        model.addAttribute("fromDateTime", fromDateTimeStr != null ? fromDateTimeStr : "");
        model.addAttribute("toDateTime", toDateTimeStr != null ? toDateTimeStr : "");
        return "shipper/tasks";
    }

    // ==========================================
    // 3. TASK DETAIL
    // ==========================================

    @GetMapping("/tasks/{id}")
    public String showTaskDetail(
            @PathVariable("id") Long id,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User shipper;
        try {
            shipper = requireShipperUser(session);
        } catch (SecurityException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập!");
            return "redirect:/login";
        }

        try {
            Shipment shipment = shipperOrderService.getShipmentDetail(shipper, id);
            ShipperDashboardDto dashboard = shipperOrderService.getDashboardData(shipper);
            java.util.List<example.com.ltw.order.entity.OrderItem> orderItems = shipperOrderService.getOrderItems(shipment.getOrder().getId());
            model.addAttribute("shipper", shipper);
            model.addAttribute("currentUser", shipper);
            model.addAttribute("dashboard", dashboard);
            model.addAttribute("shipment", shipment);
            model.addAttribute("order", shipment.getOrder());
            model.addAttribute("orderItems", orderItems);
            model.addAttribute("activeNav", "tasks");
            return "shipper/task-detail";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/shipper/tasks";
        }
    }

    // ==========================================
    // 4. RECEIVE PACKAGE (PENDING -> RECEIVED)
    // ==========================================

    @PostMapping("/tasks/{id}/receive")
    public String receiveShipment(
            @PathVariable("id") Long id,
            @RequestParam(name = "redirect", required = false) String redirectUrl,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User shipper;
        try {
            shipper = requireShipperUser(session);
        } catch (SecurityException ex) {
            return "redirect:/login";
        }

        try {
            Shipment updated = shipperOrderService.receiveShipment(shipper, id);
            String ghnCode = updated.getGhnOrderCode();
            String ghnPart = (ghnCode != null && !ghnCode.isBlank())
                    ? " | Mã vận đơn GHN: " + ghnCode
                    : "";
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã nhận đơn #" + updated.getOrder().getOrderCode() + " thành công!" + ghnPart + ". Bắt đầu đi giao hàng.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        if (redirectUrl != null && !redirectUrl.isBlank()) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/shipper/tasks/" + id;
    }

    // ==========================================
    // 5. COMPLETE DELIVERY (RECEIVED -> DELIVERED)
    // ==========================================

    @PostMapping("/tasks/{id}/complete")
    public String completeDelivery(
            @PathVariable("id") Long id,
            @RequestParam(name = "redirect", required = false) String redirectUrl,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User shipper;
        try {
            shipper = requireShipperUser(session);
        } catch (SecurityException ex) {
            return "redirect:/login";
        }

        try {
            Shipment updated = shipperOrderService.completeDelivery(shipper, id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Xác nhận giao thành công đơn hàng #" + updated.getOrder().getOrderCode() + "!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        if (redirectUrl != null && !redirectUrl.isBlank()) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/shipper/tasks/" + id;
    }

    // ==========================================
    // 6. FAIL DELIVERY WITH REASON
    // ==========================================

    @PostMapping("/tasks/{id}/fail")
    public String failDelivery(
            @PathVariable("id") Long id,
            @RequestParam(name = "reason", required = false, defaultValue = "Khách hàng không nghe máy") String reason,
            @RequestParam(name = "redirect", required = false) String redirectUrl,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User shipper;
        try {
            shipper = requireShipperUser(session);
        } catch (SecurityException ex) {
            return "redirect:/login";
        }

        try {
            Shipment updated = shipperOrderService.failDelivery(shipper, id, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã cập nhật giao thất bại cho đơn #" + updated.getOrder().getOrderCode() + "!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        if (redirectUrl != null && !redirectUrl.isBlank()) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/shipper/tasks/" + id;
    }

    // ==========================================
    // 7. RETRY DELIVERY (DELIVERY_FAILED -> RECEIVED)
    // ==========================================

    @PostMapping("/tasks/{id}/retry")
    public String retryDelivery(
            @PathVariable("id") Long id,
            @RequestParam(name = "redirect", required = false) String redirectUrl,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User shipper;
        try {
            shipper = requireShipperUser(session);
        } catch (SecurityException ex) {
            return "redirect:/login";
        }

        try {
            Shipment updated = shipperOrderService.retryDelivery(shipper, id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã kích hoạt giao lại cho đơn #" + updated.getOrder().getOrderCode() + "! Trạng thái: Đang đi giao.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        if (redirectUrl != null && !redirectUrl.isBlank()) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/shipper/tasks/" + id;
    }

    // ==========================================
    // 8. UPDATE SHIPMENT STATUS DIRECTLY
    // ==========================================

    @PostMapping("/tasks/{id}/update-status")
    public String updateTaskStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") ShipmentStatus status,
            @RequestParam(name = "reason", required = false) String reason,
            @RequestParam(name = "redirect", required = false) String redirectUrl,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User shipper;
        try {
            shipper = requireShipperUser(session);
        } catch (SecurityException ex) {
            return "redirect:/login";
        }

        try {
            Shipment updated = shipperOrderService.updateStatus(shipper, id, status, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã cập nhật trạng thái đơn #" + updated.getOrder().getOrderCode() + " thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        if (redirectUrl != null && !redirectUrl.isBlank()) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/shipper/tasks/" + id;
    }

}
