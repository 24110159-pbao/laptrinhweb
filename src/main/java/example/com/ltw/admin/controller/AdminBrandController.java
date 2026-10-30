package example.com.ltw.admin.controller;

import example.com.ltw.admin.dto.AdminBrandForm;
import example.com.ltw.admin.service.AdminProductService;
import example.com.ltw.admin.service.AdminUserService;
import example.com.ltw.catalog.entity.Brand;
import example.com.ltw.catalog.entity.BrandStatus;
import example.com.ltw.user.entity.User;
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
@RequestMapping("/admin/brands")
@RequiredArgsConstructor
public class AdminBrandController {

    private final AdminProductService adminProductService;
    private final AdminUserService adminUserService;

    @GetMapping
    public String list(HttpSession session, Model model) {
        model.addAttribute("admin", adminUserService.requireAdmin(session));
        model.addAttribute("brands", adminProductService.getAllBrands());
        return "admin/brand-list";
    }

    @GetMapping("/new")
    public String createForm(HttpSession session, Model model) {
        AdminBrandForm form = new AdminBrandForm();
        form.setStatus(BrandStatus.ACTIVE);
        model.addAttribute("admin", adminUserService.requireAdmin(session));
        model.addAttribute("form", form);
        model.addAttribute("statuses", BrandStatus.values());
        model.addAttribute("isEdit", false);
        return "admin/brand-form";
    }

    @PostMapping("/new")
    public String create(
            @Valid @ModelAttribute("form") AdminBrandForm form,
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
            adminProductService.createBrand(form);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm thương hiệu thành công.");
            return "redirect:/admin/brands";
        } catch (ResponseStatusException | DataIntegrityViolationException ex) {
            String message = ex instanceof ResponseStatusException status
                    ? status.getReason() : "Tên thương hiệu đã tồn tại.";
            return renderForm(model, admin, form, false, message);
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, HttpSession session, Model model) {
        Brand brand = adminProductService.getBrandById(id);
        AdminBrandForm form = new AdminBrandForm();
        form.setId(brand.getId());
        form.setName(brand.getName());
        form.setLogoUrl(brand.getLogoUrl());
        form.setStatus(brand.getStatus());
        model.addAttribute("admin", adminUserService.requireAdmin(session));
        model.addAttribute("form", form);
        model.addAttribute("statuses", BrandStatus.values());
        model.addAttribute("isEdit", true);
        return "admin/brand-form";
    }

    @PostMapping("/{id}/edit")
    public String edit(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") AdminBrandForm form,
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
            adminProductService.updateBrand(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thương hiệu thành công.");
            return "redirect:/admin/brands";
        } catch (ResponseStatusException | DataIntegrityViolationException ex) {
            String message = ex instanceof ResponseStatusException status
                    ? status.getReason() : "Tên thương hiệu đã tồn tại.";
            return renderForm(model, admin, form, true, message);
        }
    }

    private String renderForm(
            Model model, example.com.ltw.user.entity.User admin,
            AdminBrandForm form, boolean edit, String error
    ) {
        model.addAttribute("admin", admin);
        model.addAttribute("form", form);
        model.addAttribute("statuses", BrandStatus.values());
        model.addAttribute("isEdit", edit);
        if (error != null) {
            model.addAttribute("errorMessage", error);
        }
        return "admin/brand-form";
    }
}
