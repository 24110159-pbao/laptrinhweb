package example.com.ltw.manager.controller;

import example.com.ltw.auth.service.AuthService;
import example.com.ltw.catalog.entity.ProductVariantStatus;
import example.com.ltw.manager.dto.StaffForm;
import example.com.ltw.manager.service.ManagerService;
import example.com.ltw.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final ManagerService managerService;


    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(
            HttpServletRequest request,
            Model model
    ) {
        Long managerId = managerId(request);

        User manager = managerService.getManager(managerId);

        ManagerService.DashboardSummary summary =
                managerService.getDashboardSummary(managerId);

        ManagerService.DashboardAnalytics analytics =
                managerService.getDashboardAnalytics(managerId);

        model.addAttribute("manager", manager);
        model.addAttribute("staffCount", summary.staffCount());
        model.addAttribute("inventoryCount", summary.inventoryCount());
        model.addAttribute("orderCount", summary.orderCount());

        // Hai thẻ thống kê đã có trong dashboard.html
        model.addAttribute("revenue", analytics.revenue());
        model.addAttribute(
                "pendingOrderCount",
                analytics.pendingOrderCount()
        );

        // Dữ liệu cho biểu đồ
        model.addAttribute(
                "orderStatusCounts",
                analytics.orderStatusCounts()
        );

        model.addAttribute(
                "monthlyRevenueLabels",
                analytics.monthlyRevenueLabels()
        );

        model.addAttribute(
                "monthlyRevenueValues",
                analytics.monthlyRevenueValues()
        );
        model.addAttribute(
                "monthlyOrderValues",
                analytics.monthlyOrderValues()
        );

        return "manager/dashboard";
    }


    @GetMapping("/inventory")
    public String inventory(HttpServletRequest request, Model model) {
        Long managerId = managerId(request);
        model.addAttribute("manager", managerService.getManager(managerId));
        model.addAttribute("inventory", managerService.getInventory(managerId));
        model.addAttribute("statuses", ProductVariantStatus.values());
        return "manager/inventory";
    }

    @PostMapping("/inventory/{inventoryId}/status")
    public String updateInventoryStatus(
            @PathVariable Long inventoryId,
            @RequestParam ProductVariantStatus status,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        managerService.updateInventoryStatus(managerId(request), inventoryId, status);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái tồn kho.");
        return "redirect:/manager/inventory";
    }

    @GetMapping("/staff")
    public String staff(HttpServletRequest request, Model model) {
        Long managerId = managerId(request);
        model.addAttribute("manager", managerService.getManager(managerId));
        model.addAttribute("staffList", managerService.getStaff(managerId));
        return "manager/staff-list";
    }

    @GetMapping("/staff/new")
    public String newStaff(HttpServletRequest request, Model model) {
        model.addAttribute("manager", managerService.getManager(managerId(request)));
        model.addAttribute("staffForm", new StaffForm());
        model.addAttribute("staffId", null);
        return "manager/staff-form";
    }

    @PostMapping("/staff")
    public String createStaff(
            @Valid @ModelAttribute("staffForm") StaffForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Long managerId = managerId(request);
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            bindingResult.rejectValue("password", "NotBlank", "Vui lòng nhập mật khẩu ban đầu.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("manager", managerService.getManager(managerId));
            model.addAttribute("staffId", null);
            return "manager/staff-form";
        }

        try {
            managerService.createStaff(managerId, form);
        } catch (ManagerService.DuplicateEmailException exception) {
            bindingResult.rejectValue("email", "Duplicate", "Email này đã được sử dụng.");
            model.addAttribute("manager", managerService.getManager(managerId));
            model.addAttribute("staffId", null);
            return "manager/staff-form";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Đã thêm Staff vào chi nhánh.");
        return "redirect:/manager/staff";
    }

    @GetMapping("/staff/{staffId}/edit")
    public String editStaff(
            @PathVariable Long staffId,
            HttpServletRequest request,
            Model model
    ) {
        Long managerId = managerId(request);
        User staff = managerService.getStaff(managerId, staffId);
        StaffForm form = new StaffForm();
        form.setFullName(staff.getFullName());
        form.setEmail(staff.getEmail());
        form.setPhone(staff.getPhone());

        model.addAttribute("manager", managerService.getManager(managerId));
        model.addAttribute("staffForm", form);
        model.addAttribute("staffId", staffId);
        return "manager/staff-form";
    }

    @PostMapping("/staff/{staffId}")
    public String updateStaff(
            @PathVariable Long staffId,
            @Valid @ModelAttribute("staffForm") StaffForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Long managerId = managerId(request);
        if (bindingResult.hasErrors()) {
            model.addAttribute("manager", managerService.getManager(managerId));
            model.addAttribute("staffId", staffId);
            return "manager/staff-form";
        }

        try {
            managerService.updateStaff(managerId, staffId, form);
        } catch (ManagerService.DuplicateEmailException exception) {
            bindingResult.rejectValue("email", "Duplicate", "Email này đã được sử dụng.");
            model.addAttribute("manager", managerService.getManager(managerId));
            model.addAttribute("staffId", staffId);
            return "manager/staff-form";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin Staff.");
        return "redirect:/manager/staff";
    }

    @PostMapping("/staff/{staffId}/status")
    public String toggleStaffStatus(
            @PathVariable Long staffId,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        UserStatusMessage statusMessage = managerService.toggleStaffStatus(
                managerId(request),
                staffId
        ) == example.com.ltw.user.entity.UserStatus.LOCKED
                ? UserStatusMessage.LOCKED
                : UserStatusMessage.ACTIVE;
        redirectAttributes.addFlashAttribute(
                "successMessage",
                statusMessage.message
        );
        return "redirect:/manager/staff";
    }

    @GetMapping("/orders")
    public String orders(HttpServletRequest request, Model model) {
        Long managerId = managerId(request);
        model.addAttribute("manager", managerService.getManager(managerId));
        model.addAttribute("orders", managerService.getOrders(managerId));
        return "manager/orders";
    }

    @GetMapping("/orders/{orderId}")
    public String orderDetails(
            @PathVariable Long orderId,
            HttpServletRequest request,
            Model model
    ) {
        Long managerId = managerId(request);
        ManagerService.OrderDetails details =
                managerService.getOrderDetails(managerId, orderId);
        model.addAttribute("manager", managerService.getManager(managerId));
        model.addAttribute("order", details.order());
        model.addAttribute("items", details.items());
        return "manager/order-detail";
    }

    private Long managerId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object currentUser = session == null
                ? null
                : session.getAttribute(AuthService.SESSION_CURRENT_USER);
        if (!(currentUser instanceof User user) || user.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Vui lòng đăng nhập để truy cập khu vực Manager."
            );
        }
        return user.getId();
    }

    private enum UserStatusMessage {
        LOCKED("Đã khóa tài khoản Staff."),
        ACTIVE("Đã mở khóa tài khoản Staff.");

        private final String message;

        UserStatusMessage(String message) {
            this.message = message;
        }
    }
    // =========================
    // CUSTOMERS
    // =========================

    @GetMapping("/customers")
    public String customers(
            HttpServletRequest request,
            Model model
    ) {
        Long managerId = managerId(request);

        model.addAttribute(
                "manager",
                managerService.getManager(managerId)
        );

        model.addAttribute(
                "customers",
                managerService.getCustomers(managerId)
        );

        return "manager/customers";
    }
}
