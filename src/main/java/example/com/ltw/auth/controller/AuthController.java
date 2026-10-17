package example.com.ltw.auth.controller;

import example.com.ltw.auth.dto.LoginRequest;
import example.com.ltw.auth.dto.RegisterRequest;
import example.com.ltw.auth.dto.ResetPasswordRequest;
import example.com.ltw.auth.service.AuthService;
import example.com.ltw.user.entity.User;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ==========================================
    // LOGIN
    // ==========================================

    @GetMapping("/login")
    public String showLoginForm(Model model, HttpSession session) {
        if (session.getAttribute(AuthService.SESSION_CURRENT_USER) != null) {
            return "redirect:/";
        }
        if (!model.containsAttribute("loginRequest")) {
            model.addAttribute("loginRequest", new LoginRequest());
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String handleLogin(
            @Valid @ModelAttribute("loginRequest") LoginRequest request,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }

        try {
            User user = authService.login(request, session);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đăng nhập thành công với quyền " + user.getRole().name() + "! Xin chào mừng " + user.getFullName() + ".");
            return "redirect:/";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/login";
        } catch (Exception ex) {
            log.error("Login unexpected error", ex);
            model.addAttribute("errorMessage", "Đã có lỗi xảy ra. Vui lòng thử lại!");
            return "auth/login";
        }
    }

    // ==========================================
    // REGISTER
    // ==========================================

    @GetMapping("/register")
    public String showRegisterForm(Model model, HttpSession session) {
        if (session.getAttribute(AuthService.SESSION_CURRENT_USER) != null) {
            return "redirect:/";
        }
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegister(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            authService.startRegistration(request, session);
            redirectAttributes.addFlashAttribute("infoMessage",
                    "Mã OTP đã được gửi đến email " + request.getEmail() + ". Vui lòng kiểm tra hộp thư!");
            return "redirect:/verify-otp";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register";
        } catch (Exception ex) {
            log.error("Register unexpected error", ex);
            model.addAttribute("errorMessage", "Không thể gửi email OTP: " + ex.getMessage());
            return "auth/register";
        }
    }

    // ==========================================
    // VERIFY OTP
    // ==========================================

    @GetMapping("/verify-otp")
    public String showVerifyOtpForm(Model model, HttpSession session) {
        String email = (String) session.getAttribute(AuthService.SESSION_PENDING_EMAIL);
        if (email == null) {
            return "redirect:/register";
        }
        model.addAttribute("email", email);
        return "auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String handleVerifyOtp(
            @RequestParam("email") String email,
            @RequestParam("otp") String otp,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        try {
            User user = authService.completeRegistration(email, otp, session);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đăng ký tài khoản thành công! Chào mừng " + user.getFullName() + " đến với NEXUSTECH FLAGSHIP STORE.");
            return "redirect:/";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("email", email);
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/verify-otp";
        } catch (Exception ex) {
            log.error("OTP verification error", ex);
            model.addAttribute("email", email);
            model.addAttribute("errorMessage", "Lỗi xác thực: " + ex.getMessage());
            return "auth/verify-otp";
        }
    }

    @PostMapping("/resend-otp")
    public String handleResendOtp(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            authService.resendRegistrationOtp(session);
            redirectAttributes.addFlashAttribute("infoMessage", "Mã xác thực mới đã được gửi lại vào email của bạn!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/verify-otp";
    }

    // ==========================================
    // FORGOT / RESET PASSWORD
    // ==========================================

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(
            @RequestParam("email") String email,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        try {
            authService.startForgotPassword(email, session);
            redirectAttributes.addFlashAttribute("infoMessage",
                    "Mã OTP khôi phục mật khẩu đã được gửi đến email " + email);
            return "redirect:/reset-password";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("email", email);
            return "auth/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(Model model, HttpSession session) {
        String email = (String) session.getAttribute(AuthService.SESSION_RESET_EMAIL);
        if (email == null) {
            return "redirect:/forgot-password";
        }
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(email);
        model.addAttribute("resetPasswordRequest", request);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(
            @Valid @ModelAttribute("resetPasswordRequest") ResetPasswordRequest request,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/reset-password";
        }

        try {
            authService.completeForgotPassword(request, session);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đổi mật khẩu thành công! Vui lòng đăng nhập bằng mật khẩu mới.");
            return "redirect:/login";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/reset-password";
        }
    }

    // ==========================================
    // LOGOUT
    // ==========================================

    @GetMapping("/logout")
    public String handleLogout(HttpSession session, RedirectAttributes redirectAttributes) {
        authService.logout(session);
        redirectAttributes.addFlashAttribute("infoMessage", "Bạn đã đăng xuất khỏi tài khoản.");
        return "redirect:/";
    }
}
