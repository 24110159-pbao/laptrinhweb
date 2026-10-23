package example.com.ltw.admin.controller;

import example.com.ltw.admin.dto.AdminUserForm;
import example.com.ltw.admin.service.AdminBranchService;
import example.com.ltw.admin.service.AdminUserService;
import example.com.ltw.user.entity.Role;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.entity.UserStatus;
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
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AdminBranchService adminBranchService;

    // ==========================================
    // USERS LIST & FILTER
    // ==========================================

    @GetMapping("/users")
    public String listUsers(
            @RequestParam(value = "role", required = false) Role role,
            @RequestParam(value = "keyword", required = false) String keyword,
            HttpSession session,
            Model model
    ) {
        User admin = adminUserService.requireAdmin(session);

        model.addAttribute("admin", admin);
        model.addAttribute("users", adminUserService.getUsers(role, keyword));
        model.addAttribute("selectedRole", role);
        model.addAttribute("keyword", keyword);
        model.addAttribute("roles", Role.values());
        model.addAttribute("statuses", UserStatus.values());

        return "admin/user-list";
    }

    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User admin = adminUserService.requireAdmin(session);
        try {
            UserStatus newStatus = adminUserService.toggleUserStatus(id, admin.getId());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật trạng thái người dùng thành: " + newStatus);
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getReason());
        }
        return "redirect:/admin/users";
    }

    // ==========================================
    // MANAGERS MANAGEMENT
    // ==========================================

    @GetMapping("/managers")
    public String listManagers(HttpSession session, Model model) {
        User admin = adminUserService.requireAdmin(session);

        model.addAttribute("admin", admin);
        model.addAttribute("managers", adminUserService.getManagers());
        model.addAttribute("branches", adminBranchService.getAllBranches());

        return "admin/manager-list";
    }

    @GetMapping("/managers/new")
    public String showCreateManagerForm(HttpSession session, Model model) {
        User admin = adminUserService.requireAdmin(session);

        AdminUserForm form = new AdminUserForm();
        form.setRole(Role.MANAGER);
        form.setStatus(UserStatus.ACTIVE);

        model.addAttribute("admin", admin);
        model.addAttribute("form", form);
        model.addAttribute("isEdit", false);
        model.addAttribute("branches", adminBranchService.getAllBranches());

        return "admin/manager-form";
    }

    @PostMapping("/managers/new")
    public String handleCreateManager(
            @Valid @ModelAttribute("form") AdminUserForm form,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        User admin = adminUserService.requireAdmin(session);

        if (bindingResult.hasErrors()) {
            model.addAttribute("admin", admin);
            model.addAttribute("isEdit", false);
            model.addAttribute("branches", adminBranchService.getAllBranches());
            return "admin/manager-form";
        }

        try {
            adminUserService.createManager(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Tạo tài khoản Manager cho " + form.getFullName() + " thành công!");
            return "redirect:/admin/managers";
        } catch (ResponseStatusException ex) {
            model.addAttribute("admin", admin);
            model.addAttribute("errorMessage", ex.getReason());
            model.addAttribute("isEdit", false);
            model.addAttribute("branches", adminBranchService.getAllBranches());
            return "admin/manager-form";
        }
    }

    @GetMapping("/managers/{id}/edit")
    public String showEditManagerForm(
            @PathVariable Long id,
            HttpSession session,
            Model model
    ) {
        User admin = adminUserService.requireAdmin(session);
        User manager = adminUserService.getUserById(id);

        AdminUserForm form = new AdminUserForm();
        form.setId(manager.getId());
        form.setFullName(manager.getFullName());
        form.setEmail(manager.getEmail());
        form.setPhone(manager.getPhone());
        form.setRole(manager.getRole());
        form.setStatus(manager.getStatus());
        if (manager.getBranch() != null) {
            form.setBranchId(manager.getBranch().getId());
        }

        model.addAttribute("admin", admin);
        model.addAttribute("form", form);
        model.addAttribute("isEdit", true);
        model.addAttribute("manager", manager);
        model.addAttribute("branches", adminBranchService.getAllBranches());

        return "admin/manager-form";
    }

    @PostMapping("/managers/{id}/edit")
    public String handleEditManager(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") AdminUserForm form,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        User admin = adminUserService.requireAdmin(session);

        if (bindingResult.hasErrors()) {
            model.addAttribute("admin", admin);
            model.addAttribute("isEdit", true);
            model.addAttribute("branches", adminBranchService.getAllBranches());
            return "admin/manager-form";
        }

        try {
            adminUserService.updateManager(id, form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật tài khoản Manager thành công!");
            return "redirect:/admin/managers";
        } catch (ResponseStatusException ex) {
            model.addAttribute("admin", admin);
            model.addAttribute("errorMessage", ex.getReason());
            model.addAttribute("isEdit", true);
            model.addAttribute("branches", adminBranchService.getAllBranches());
            return "admin/manager-form";
        }
    }
}
