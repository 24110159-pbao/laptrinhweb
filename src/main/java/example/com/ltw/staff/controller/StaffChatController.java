package example.com.ltw.staff.controller;

import example.com.ltw.auth.service.AuthService;
import example.com.ltw.chat.entity.ChatConversation;
import example.com.ltw.chat.service.ChatService;
import example.com.ltw.staff.dto.StaffDashboardDto;
import example.com.ltw.staff.service.StaffOrderService;
import example.com.ltw.user.entity.Role;
import example.com.ltw.user.entity.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffChatController {

    private final ChatService chatService;
    private final StaffOrderService staffOrderService;

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

    @GetMapping("/chat")
    public String showChatCenter(
            @RequestParam(name = "conversationId", required = false) Long conversationId,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User staff;
        try {
            staff = requireStaffUser(session);
        } catch (SecurityException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập tài khoản nhân viên!");
            return "redirect:/login";
        }

        StaffDashboardDto dashboard = staffOrderService.getDashboardData(staff);
        List<ChatConversation> conversations = chatService.getBranchConversations(dashboard.getBranchId());

        ChatConversation activeConversation = null;
        if (conversationId != null) {
            activeConversation = conversations.stream()
                    .filter(c -> c.getId().equals(conversationId))
                    .findFirst()
                    .orElse(null);
        }
        if (activeConversation == null && !conversations.isEmpty()) {
            activeConversation = conversations.get(0);
        }

        model.addAttribute("staff", staff);
        model.addAttribute("currentUser", staff);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("conversations", conversations);
        model.addAttribute("activeConversation", activeConversation);
        model.addAttribute("locationName", dashboard.getBranchName());
        model.addAttribute("activeNav", "chat");
        return "staff/chat";
    }
}
