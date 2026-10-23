package example.com.ltw.admin.controller;

import example.com.ltw.admin.dto.AdminBranchForm;
import example.com.ltw.admin.service.AdminBranchService;
import example.com.ltw.admin.service.AdminUserService;
import example.com.ltw.branch.entity.Branch;
import example.com.ltw.branch.entity.BranchStatus;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/branches")
@RequiredArgsConstructor
public class AdminBranchController {

    private final AdminBranchService adminBranchService;
    private final AdminUserService adminUserService;

    @GetMapping
    public String listBranches(HttpSession session, Model model) {
        User admin = adminUserService.requireAdmin(session);

        model.addAttribute("admin", admin);
        model.addAttribute("branches", adminBranchService.getAllBranches());
        model.addAttribute("statuses", BranchStatus.values());

        return "admin/branch-list";
    }

    @GetMapping("/new")
    public String showCreateForm(HttpSession session, Model model) {
        User admin = adminUserService.requireAdmin(session);

        AdminBranchForm form = new AdminBranchForm();
        form.setStatus(BranchStatus.ACTIVE);

        model.addAttribute("admin", admin);
        model.addAttribute("form", form);
        model.addAttribute("isEdit", false);
        model.addAttribute("statuses", BranchStatus.values());

        return "admin/branch-form";
    }

    @PostMapping("/new")
    public String handleCreateBranch(
            @Valid @ModelAttribute("form") AdminBranchForm form,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        User admin = adminUserService.requireAdmin(session);

        if (bindingResult.hasErrors()) {
            model.addAttribute("admin", admin);
            model.addAttribute("isEdit", false);
            model.addAttribute("statuses", BranchStatus.values());
            return "admin/branch-form";
        }

        try {
            adminBranchService.createBranch(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Thêm chi nhánh '" + form.getName() + "' thành công!");
            return "redirect:/admin/branches";
        } catch (ResponseStatusException ex) {
            model.addAttribute("admin", admin);
            model.addAttribute("errorMessage", ex.getReason());
            model.addAttribute("isEdit", false);
            model.addAttribute("statuses", BranchStatus.values());
            return "admin/branch-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            HttpSession session,
            Model model
    ) {
        User admin = adminUserService.requireAdmin(session);
        Branch branch = adminBranchService.getBranchById(id);

        AdminBranchForm form = new AdminBranchForm();
        form.setId(branch.getId());
        form.setName(branch.getName());
        form.setAddress(branch.getAddress());
        form.setPhone(branch.getPhone());
        form.setEmail(branch.getEmail());
        form.setLatitude(branch.getLatitude());
        form.setLongitude(branch.getLongitude());
        form.setStatus(branch.getStatus());

        model.addAttribute("admin", admin);
        model.addAttribute("form", form);
        model.addAttribute("isEdit", true);
        model.addAttribute("branch", branch);
        model.addAttribute("statuses", BranchStatus.values());

        return "admin/branch-form";
    }

    @PostMapping("/{id}/edit")
    public String handleEditBranch(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") AdminBranchForm form,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        User admin = adminUserService.requireAdmin(session);

        if (bindingResult.hasErrors()) {
            model.addAttribute("admin", admin);
            model.addAttribute("isEdit", true);
            model.addAttribute("statuses", BranchStatus.values());
            return "admin/branch-form";
        }

        try {
            adminBranchService.updateBranch(id, form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật chi nhánh '" + form.getName() + "' thành công!");
            return "redirect:/admin/branches";
        } catch (ResponseStatusException ex) {
            model.addAttribute("admin", admin);
            model.addAttribute("errorMessage", ex.getReason());
            model.addAttribute("isEdit", true);
            model.addAttribute("statuses", BranchStatus.values());
            return "admin/branch-form";
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
            BranchStatus newStatus = adminBranchService.toggleBranchStatus(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Trạng thái chi nhánh đã được chuyển sang: " + newStatus);
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getReason());
        }
        return "redirect:/admin/branches";
    }
}
