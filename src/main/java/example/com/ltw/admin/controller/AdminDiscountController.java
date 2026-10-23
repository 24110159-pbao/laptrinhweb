package example.com.ltw.admin.controller;

import example.com.ltw.admin.dto.AdminDiscountForm;
import example.com.ltw.admin.service.AdminDiscountService;
import example.com.ltw.admin.service.AdminUserService;
import example.com.ltw.discount.entity.DiscountCode;
import example.com.ltw.discount.entity.DiscountStatus;
import example.com.ltw.user.entity.User;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/admin/discounts")
@RequiredArgsConstructor
public class AdminDiscountController {

    private final AdminDiscountService adminDiscountService;
    private final AdminUserService adminUserService;

    @GetMapping
    public String listDiscounts(
            @RequestParam(value = "status", required = false) DiscountStatus status,
            HttpSession session,
            Model model
    ) {
        User admin = adminUserService.requireAdmin(session);

        model.addAttribute("admin", admin);
        model.addAttribute("discounts", adminDiscountService.getAllDiscounts(status));
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", DiscountStatus.values());

        return "admin/discount-list";
    }

    @GetMapping("/new")
    public String showCreateForm(HttpSession session, Model model) {
        User admin = adminUserService.requireAdmin(session);

        AdminDiscountForm form = new AdminDiscountForm();
        form.setStatus(DiscountStatus.ACTIVE);

        model.addAttribute("admin", admin);
        model.addAttribute("form", form);
        model.addAttribute("statuses", DiscountStatus.values());
        model.addAttribute("isEdit", false);

        return "admin/discount-form";
    }

    @PostMapping("/new")
    public String handleCreateDiscount(
            @Valid @ModelAttribute("form") AdminDiscountForm form,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        User admin = adminUserService.requireAdmin(session);

        if (bindingResult.hasErrors()) {
            model.addAttribute("admin", admin);
            model.addAttribute("statuses", DiscountStatus.values());
            model.addAttribute("isEdit", false);
            return "admin/discount-form";
        }

        try {
            adminDiscountService.createDiscount(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Thêm mã giảm giá '" + form.getCode() + "' thành công!");
            return "redirect:/admin/discounts";
        } catch (ResponseStatusException ex) {
            model.addAttribute("admin", admin);
            model.addAttribute("errorMessage", ex.getReason());
            model.addAttribute("statuses", DiscountStatus.values());
            model.addAttribute("isEdit", false);
            return "admin/discount-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            HttpSession session,
            Model model
    ) {
        User admin = adminUserService.requireAdmin(session);
        DiscountCode discount = adminDiscountService.getDiscountById(id);

        AdminDiscountForm form = new AdminDiscountForm();
        form.setId(discount.getId());
        form.setCode(discount.getCode());
        form.setDiscountValue(discount.getDiscountValue());
        form.setQuantity(discount.getQuantity());
        form.setStartAt(discount.getStartAt());
        form.setEndAt(discount.getEndAt());
        form.setStatus(discount.getStatus());

        model.addAttribute("admin", admin);
        model.addAttribute("form", form);
        model.addAttribute("discount", discount);
        model.addAttribute("statuses", DiscountStatus.values());
        model.addAttribute("isEdit", true);

        return "admin/discount-form";
    }

    @PostMapping("/{id}/edit")
    public String handleEditDiscount(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") AdminDiscountForm form,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        User admin = adminUserService.requireAdmin(session);

        if (bindingResult.hasErrors()) {
            model.addAttribute("admin", admin);
            model.addAttribute("statuses", DiscountStatus.values());
            model.addAttribute("isEdit", true);
            return "admin/discount-form";
        }

        try {
            adminDiscountService.updateDiscount(id, form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật mã giảm giá '" + form.getCode() + "' thành công!");
            return "redirect:/admin/discounts";
        } catch (ResponseStatusException ex) {
            model.addAttribute("admin", admin);
            model.addAttribute("errorMessage", ex.getReason());
            model.addAttribute("statuses", DiscountStatus.values());
            model.addAttribute("isEdit", true);
            return "admin/discount-form";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        adminUserService.requireAdmin(session);
        try {
            DiscountStatus newStatus = adminDiscountService.toggleDiscountStatus(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Trạng thái mã giảm giá đã chuyển sang: " + newStatus);
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getReason());
        }
        return "redirect:/admin/discounts";
    }
}
