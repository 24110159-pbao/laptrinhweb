package example.com.ltw.catalog.controller;

import example.com.ltw.auth.service.AuthService;
import example.com.ltw.cart.dto.CartView;
import example.com.ltw.cart.service.CartService;
import example.com.ltw.catalog.dto.ProductCardDto;
import example.com.ltw.catalog.service.ProductService;
import example.com.ltw.user.entity.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.NumberFormat;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CartService cartService;

    @GetMapping({"/", "/home"})
    public String index(
            @RequestParam(value = "brand", required = false, defaultValue = "all") String brand,
            @RequestParam(value = "keyword", required = false) String keyword,
            HttpSession session,
            Model model
    ) {
        List<ProductCardDto> products = productService.getFeaturedProducts(brand, keyword);
        Object sessionUser = session.getAttribute(AuthService.SESSION_CURRENT_USER);
        User currentUser = sessionUser instanceof User user ? user : null;

        model.addAttribute("products", products);
        model.addAttribute("selectedBrand", brand != null ? brand.toLowerCase() : "all");
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("currentUser", currentUser);

        // Header metrics matching the design
        model.addAttribute("locationName", "TP. Hồ Chí Minh");
        model.addAttribute("compareCount", 2);
        List<CartView> carts = currentUser == null
                ? List.of()
                : cartService.getCarts(currentUser.getId());
        int cartCount = carts.stream()
                .flatMap(cart -> cart.lines().stream())
                .mapToInt(line -> line.quantity())
                .sum();
        BigDecimal cartTotal = carts.stream()
                .map(CartView::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("cartCount", cartCount);
        model.addAttribute("cartTotal", NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
                .format(cartTotal) + "₫");

        return "home";
    }
}
