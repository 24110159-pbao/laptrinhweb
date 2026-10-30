package example.com.ltw.admin.controller;

import example.com.ltw.admin.dto.AdminDashboardSummaryDto;
import example.com.ltw.admin.dto.BranchRevenueDto;
import example.com.ltw.admin.service.AdminDashboardService;
import example.com.ltw.admin.service.AdminUserService;
import example.com.ltw.user.entity.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final AdminUserService adminUserService;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(HttpSession session, Model model) {
        User admin = adminUserService.requireAdmin(session);

        AdminDashboardSummaryDto summary = adminDashboardService.getDashboardSummary();

        model.addAttribute("admin", admin);
        model.addAttribute("summary", summary);

        // Core metric cards
        model.addAttribute("totalRevenue", summary.getTotalRevenue());
        model.addAttribute("totalOrders", summary.getTotalOrders());
        model.addAttribute("totalUsers", summary.getTotalUsers());
        model.addAttribute("totalBranches", summary.getTotalBranches());
        model.addAttribute("totalProducts", summary.getTotalProducts());

        // Chart datasets (matching Chart.js format like Manager dashboard)
        model.addAttribute("monthlyRevenueLabels", summary.getMonthlyLabels());
        model.addAttribute("monthlyRevenueValues", summary.getMonthlyRevenues());
        model.addAttribute("monthlyOrderValues", summary.getMonthlyOrders());
        model.addAttribute("orderStatusCounts", summary.getOrderStatusCounts());

        // Revenue per branch
        model.addAttribute("branchRevenues", summary.getBranchRevenues());

        return "admin/dashboard";
    }

    @GetMapping("/branch-revenues")
    public String branchRevenues(HttpSession session, Model model) {
        User admin = adminUserService.requireAdmin(session);
        List<BranchRevenueDto> branchRevenues = adminDashboardService.getBranchRevenues();

        model.addAttribute("admin", admin);
        model.addAttribute("branchRevenues", branchRevenues);

        return "admin/branch-revenues";
    }
}
