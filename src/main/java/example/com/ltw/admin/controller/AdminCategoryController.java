package example.com.ltw.admin.controller;

import example.com.ltw.admin.dto.AdminCategoryForm;
import example.com.ltw.admin.service.AdminProductService;
import example.com.ltw.admin.service.AdminUserService;
import example.com.ltw.catalog.entity.Category;
import example.com.ltw.catalog.entity.CategoryStatus;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminProductService adminProductService;
    private final AdminUserService adminUserService;

    @GetMapping
    public String list(HttpSession session, Model model) {
        model.addAttribute("admin", adminUserService.requireAdmin(session));
        model.addAttribute("categories", adminProductService.getAllCategories());
        return "admin/category-list";
    }

    @GetMapping("/new")
    public String createForm(HttpSession session, Model model) {
        AdminCategoryForm form = new AdminCategoryForm();
        form.setStatus(CategoryStatus.ACTIVE);
        model.addAttribute("admin", adminUserService.requireAdmin(session));
        model.addAttribute("form", form);
        model.addAttribute("statuses", CategoryStatus.values());
        model.addAttribute("isEdit", false);
        return "admin/category-form";
    }

    @PostMapping("/new")
    public String create(
            @Valid @ModelAttribute("form") AdminCategoryForm form,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        var admin = adminUserService.requireAdmin(session);
        if (bindingResult.hasErrors()) {
            return renderForm(model, admin, form, false, null);
        }
        try {
            adminProductService.createCategory(form);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm danh mục thành công.");
            return "redirect:/admin/categories";
        } catch (ResponseStatusException | DataIntegrityViolationException ex) {
            String message = ex instanceof ResponseStatusException status
                    ? status.getReason() : "Slug danh mục đã tồn tại.";
            return renderForm(model, admin, form, false, message);
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, HttpSession session, Model model) {
        Category category = adminProductService.getCategoryById(id);
        AdminCategoryForm form = new AdminCategoryForm();
        form.setId(category.getId());
        form.setName(category.getName());
        form.setSlug(category.getSlug());
        form.setDescription(category.getDescription());
        form.setStatus(category.getStatus());
        model.addAttribute("admin", adminUserService.requireAdmin(session));
        model.addAttribute("form", form);
        model.addAttribute("statuses", CategoryStatus.values());
        model.addAttribute("isEdit", true);
        return "admin/category-form";
    }

    @PostMapping("/{id}/edit")
    public String edit(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") AdminCategoryForm form,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        var admin = adminUserService.requireAdmin(session);
        if (bindingResult.hasErrors()) {
            return renderForm(model, admin, form, true, null);
        }
        try {
            adminProductService.updateCategory(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật danh mục thành công.");
            return "redirect:/admin/categories";
        } catch (ResponseStatusException | DataIntegrityViolationException ex) {
            String message = ex instanceof ResponseStatusException status
                    ? status.getReason() : "Slug danh mục đã tồn tại.";
            return renderForm(model, admin, form, true, message);
        }
    }

    private String renderForm(
            Model model, example.com.ltw.user.entity.User admin,
            AdminCategoryForm form, boolean edit, String error
    ) {
        model.addAttribute("admin", admin);
        model.addAttribute("form", form);
        model.addAttribute("statuses", CategoryStatus.values());
        model.addAttribute("isEdit", edit);
        if (error != null) {
            model.addAttribute("errorMessage", error);
        }
        return "admin/category-form";
    }
}
