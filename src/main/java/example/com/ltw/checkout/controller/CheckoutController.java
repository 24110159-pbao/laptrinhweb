package example.com.ltw.checkout.controller;

import example.com.ltw.address.dto.AddressRequest;
import example.com.ltw.auth.service.AuthService;
import example.com.ltw.cart.dto.CartView;
import example.com.ltw.cart.service.CartService;
import example.com.ltw.checkout.entity.CheckoutSession;
import example.com.ltw.checkout.service.CheckoutService;
import example.com.ltw.order.entity.PaymentMethod;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.repository.AddressRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final CartService cartService;
    private final AddressRepository addressRepository;

    @GetMapping("/checkout/{checkoutId}")
    public String showCheckout(
            @PathVariable Long checkoutId, HttpSession session, Model model,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";
        try {
            CheckoutSession checkout = checkoutService.getActive(checkoutId, userId);
            model.addAttribute("checkout", checkout);
            model.addAttribute("addresses", addressRepository
                    .findByUserIdAndDeletedAtIsNullOrderByIsDefaultDescIdDesc(userId));
            model.addAttribute("cart", cartService.getCarts(userId).stream()
                    .filter(cart -> cart.cartId().equals(checkout.getCart().getId()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Giỏ hàng không còn tồn tại.")));
            model.addAttribute("addressRequest", new AddressRequest());
            model.addAttribute("paymentMethods", PaymentMethod.values());
            return "checkout/index";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/cart";
        }
    }

    @PostMapping("/checkout/{checkoutId}/quote")
    public String calculateQuote(
            @PathVariable Long checkoutId,
            @RequestParam(required = false) Long addressId,
            @ModelAttribute AddressRequest addressRequest,
            @RequestParam(required = false) String discountCode,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";
        try {
            CheckoutSession checkout = checkoutService.quote(checkoutId, userId, addressId, addressRequest, discountCode);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận địa chỉ. Đơn hàng được miễn phí vận chuyển.");
            return "redirect:/checkout/" + checkout.getId();
        } catch (CheckoutService.CheckoutPriceChangedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/checkout/" + checkoutId;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/checkout/" + checkoutId;
        }
    }

    @PostMapping("/checkout/{checkoutId}/confirm")
    public String confirm(
            @PathVariable Long checkoutId,
            @RequestParam PaymentMethod paymentMethod,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";
        try {
            CheckoutService.FinalizeResult result = checkoutService.finalizeOrder(
                    checkoutId, userId, paymentMethod, request.getRemoteAddr()
            );
            if (result.requiresRequote()) {
                redirectAttributes.addFlashAttribute("infoMessage", result.message());
                return "redirect:/checkout/" + checkoutId;
            }
            if (result.paymentUrl() != null) return "redirect:" + result.paymentUrl();
            redirectAttributes.addFlashAttribute("successMessage", "Đặt hàng thành công. Mã đơn: " + result.orderCode());
            return "redirect:/payment/result?orderCode=" + result.orderCode();
        } catch (CheckoutService.CheckoutPriceChangedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/checkout/" + checkoutId;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/checkout/" + checkoutId;
        }
    }

    @PostMapping("/checkout/{checkoutId}/cancel")
    public String cancel(
            @PathVariable Long checkoutId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";
        try {
            checkoutService.cancel(checkoutId, userId);
            redirectAttributes.addFlashAttribute("infoMessage", "Đã hủy Checkout và giải phóng số lượng đang giữ.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/cart";
    }

    private Long currentUserId(HttpSession session) {
        Object currentUser = session.getAttribute(AuthService.SESSION_CURRENT_USER);
        return currentUser instanceof User user ? user.getId() : null;
    }
}
