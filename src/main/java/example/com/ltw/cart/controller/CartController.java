package example.com.ltw.cart.controller;

import example.com.ltw.auth.service.AuthService;
import example.com.ltw.cart.service.CartService;
import example.com.ltw.checkout.service.CheckoutService;
import example.com.ltw.user.entity.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CheckoutService checkoutService;

    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        model.addAttribute("carts", cartService.getCarts(userId));
        model.addAttribute("cartOptions", cartService.getCartOptions());
        model.addAttribute("currentUser", session.getAttribute(AuthService.SESSION_CURRENT_USER));
        return "cart/index";
    }

    @PostMapping("/cart/items")
    public String addItem(
            @RequestParam Long inventoryId,
            @RequestParam int quantity,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            cartService.addItem(userId, inventoryId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào giỏ hàng.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/items/quantity")
    public String updateQuantity(
            @RequestParam Long itemId,
            @RequestParam int quantity,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            cartService.updateQuantity(userId, itemId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Số lượng sản phẩm đã được cập nhật.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/items/remove")
    public String removeItem(
            @RequestParam Long itemId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            cartService.removeItem(userId, itemId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/{cartId}/checkout")
    public String startCheckout(
            @org.springframework.web.bind.annotation.PathVariable Long cartId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = currentUserId(session);
        if (userId == null) return "redirect:/login";
        try {
            return "redirect:/checkout/" + checkoutService.start(userId, cartId).getId();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/cart";
        }
    }

    private Long currentUserId(HttpSession session) {
        Object currentUser = session.getAttribute(AuthService.SESSION_CURRENT_USER);
        return currentUser instanceof User user ? user.getId() : null;
    }
}
