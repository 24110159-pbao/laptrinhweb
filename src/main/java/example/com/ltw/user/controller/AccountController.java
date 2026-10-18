package example.com.ltw.user.controller;

import example.com.ltw.address.dto.AddressRequest;
import example.com.ltw.address.entity.Address;
import example.com.ltw.auth.service.AuthService;
import example.com.ltw.user.dto.ChangePasswordRequest;
import example.com.ltw.user.dto.EmailChangeRequest;
import example.com.ltw.user.dto.EmailOtpRequest;
import example.com.ltw.user.dto.ProfileUpdateRequest;
import example.com.ltw.user.entity.Gender;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.service.AccountService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final AuthService authService;

    @GetMapping("/account/profile")
    public String showProfile(HttpSession session, Model model) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        User user = accountService.getUser(userId);
        model.addAttribute("currentUser", user);
        model.addAttribute("profileRequest", profileRequestFor(user));
        model.addAttribute("passwordRequest", new ChangePasswordRequest());
        model.addAttribute("emailChangeRequest", new EmailChangeRequest());
        model.addAttribute("genders", Gender.values());
        return "account/profile";
    }

    @PostMapping("/account/profile")
    public String updateProfile(
            @Valid @ModelAttribute("profileRequest") ProfileUpdateRequest request,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            populateProfilePage(userId, model, request);
            return "account/profile";
        }
        accountService.updateProfile(userId, request, session);
        redirectAttributes.addFlashAttribute("successMessage", "Thông tin hồ sơ đã được cập nhật.");
        return "redirect:/account/profile";
    }

    @PostMapping("/account/profile/avatar")
    public String updateAvatar(
            @RequestParam("avatar") MultipartFile avatar,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            accountService.updateAvatar(userId, avatar, session);
            redirectAttributes.addFlashAttribute("successMessage", "Ảnh đại diện đã được cập nhật.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/account/profile";
    }

    @PostMapping("/account/profile/password")
    public String changePassword(
            @Valid @ModelAttribute("passwordRequest") ChangePasswordRequest request,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().getFirst().getDefaultMessage());
            return "redirect:/account/profile";
        }
        try {
            accountService.changePassword(userId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Mật khẩu đã được thay đổi.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/account/profile";
    }

    @PostMapping("/account/profile/email")
    public String startEmailChange(
            @Valid @ModelAttribute("emailChangeRequest") EmailChangeRequest request,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().getFirst().getDefaultMessage());
            return "redirect:/account/profile";
        }
        try {
            authService.startEmailChange(accountService.getUser(userId), request.getEmail(), session);
            redirectAttributes.addFlashAttribute("infoMessage", "Mã OTP xác minh đã được gửi đến email mới.");
            return "redirect:/account/profile/email/verify";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/account/profile";
        }
    }

    @GetMapping("/account/profile/email/verify")
    public String showEmailOtp(HttpSession session, Model model) {
        Long userId = currentUserId(session);
        String pendingEmail = (String) session.getAttribute(AuthService.SESSION_PENDING_EMAIL_CHANGE);
        Long pendingUserId = (Long) session.getAttribute(AuthService.SESSION_PENDING_EMAIL_CHANGE_USER);
        if (userId == null) {
            return "redirect:/login";
        }
        if (pendingEmail == null || pendingUserId == null || !pendingUserId.equals(userId)) {
            return "redirect:/account/profile";
        }
        model.addAttribute("pendingEmail", pendingEmail);
        model.addAttribute("otpRequest", new EmailOtpRequest());
        return "account/verify-email";
    }

    @PostMapping("/account/profile/email/verify")
    public String verifyNewEmail(
            @Valid @ModelAttribute("otpRequest") EmailOtpRequest request,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        String pendingEmail = (String) session.getAttribute(AuthService.SESSION_PENDING_EMAIL_CHANGE);
        if (pendingEmail == null) {
            return "redirect:/account/profile";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("pendingEmail", pendingEmail);
            return "account/verify-email";
        }
        try {
            authService.completeEmailChange(accountService.getUser(userId), request.getOtp(), session);
            redirectAttributes.addFlashAttribute("successMessage", "Email tài khoản đã được xác minh và cập nhật.");
            return "redirect:/account/profile";
        } catch (RuntimeException ex) {
            model.addAttribute("pendingEmail", pendingEmail);
            model.addAttribute("errorMessage", ex.getMessage());
            return "account/verify-email";
        }
    }

    @GetMapping("/account/addresses")
    public String showAddresses(HttpSession session, Model model) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        populateAddressPage(userId, model, new AddressRequest(), null);
        return "account/addresses";
    }

    @GetMapping("/account/addresses/{addressId}")
    public String showAddressDetail(
            @PathVariable Long addressId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            Address address = accountService.getAddress(userId, addressId);
            populateAddressPage(userId, model, addressRequestFor(address), address);
            return "account/addresses";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/account/addresses";
        }
    }

    @PostMapping("/account/addresses")
    public String saveAddress(
            @Valid @ModelAttribute("addressRequest") AddressRequest request,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            populateAddressPage(userId, model, request, null);
            model.addAttribute("errorMessage", bindingResult.getAllErrors().getFirst().getDefaultMessage());
            return "account/addresses";
        }
        try {
            Address saved = accountService.saveAddress(userId, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    request.getId() == null ? "Đã thêm địa chỉ nhận hàng." : "Địa chỉ nhận hàng đã được cập nhật.");
            return "redirect:/account/addresses/" + saved.getId();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            populateAddressPage(userId, model, request, null);
            model.addAttribute("errorMessage", ex.getMessage());
            return "account/addresses";
        }
    }

    @PostMapping("/account/addresses/{addressId}/default")
    public String setDefaultAddress(
            @PathVariable Long addressId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            accountService.setDefaultAddress(userId, addressId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã đặt địa chỉ mặc định.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/account/addresses";
    }

    @PostMapping("/account/addresses/{addressId}/delete")
    public String deleteAddress(
            @PathVariable Long addressId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            accountService.deleteAddress(userId, addressId);
            redirectAttributes.addFlashAttribute("successMessage", "Địa chỉ đã được xóa khỏi sổ địa chỉ.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/account/addresses";
    }

    private Long currentUserId(HttpSession session) {
        Object sessionUser = session.getAttribute(AuthService.SESSION_CURRENT_USER);
        return sessionUser instanceof User user ? user.getId() : null;
    }

    private void populateProfilePage(Long userId, Model model, ProfileUpdateRequest request) {
        User user = accountService.getUser(userId);
        model.addAttribute("currentUser", user);
        model.addAttribute("profileRequest", request);
        model.addAttribute("genders", Gender.values());
        model.addAttribute("passwordRequest", new ChangePasswordRequest());
        model.addAttribute("emailChangeRequest", new EmailChangeRequest());
    }

    private void populateAddressPage(Long userId, Model model, AddressRequest request, Address selectedAddress) {
        List<Address> addresses = accountService.getAddresses(userId);
        model.addAttribute("currentUser", accountService.getUser(userId));
        model.addAttribute("addresses", addresses);
        model.addAttribute("addressRequest", request);
        model.addAttribute("selectedAddress", selectedAddress);
    }

    private ProfileUpdateRequest profileRequestFor(User user) {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFullName(user.getFullName());
        request.setPhone(user.getPhone());
        request.setDateOfBirth(user.getDateOfBirth());
        request.setGender(user.getGender());
        return request;
    }

    private AddressRequest addressRequestFor(Address address) {
        AddressRequest request = new AddressRequest();
        request.setId(address.getId());
        request.setReceiverName(address.getReceiverName());
        request.setPhone(address.getPhone());
        request.setProvinceId(address.getProvinceId());
        request.setProvince(address.getProvince());
        request.setDistrictId(address.getDistrictId());
        request.setDistrict(address.getDistrict());
        request.setWardCode(address.getWardCode());
        request.setWard(address.getWard());
        request.setStreetAddress(address.getStreetAddress());
        request.setMakeDefault(address.isDefault());
        return request;
    }
}
