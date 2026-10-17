package example.com.ltw.catalog.controller;

import example.com.ltw.auth.service.AuthService;
import example.com.ltw.catalog.dto.ProductCardDto;
import example.com.ltw.catalog.service.ProductService;
import example.com.ltw.user.entity.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;

    @GetMapping({"/", "/home"})
    public String index(
            @RequestParam(value = "brand", required = false, defaultValue = "all") String brand,
            @RequestParam(value = "keyword", required = false) String keyword,
            HttpSession session,
            Model model
    ) {
        List<ProductCardDto> products = productService.getFeaturedProducts(brand, keyword);
        User currentUser = (User) session.getAttribute(AuthService.SESSION_CURRENT_USER);

        model.addAttribute("products", products);
        model.addAttribute("selectedBrand", brand != null ? brand.toLowerCase() : "all");
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("currentUser", currentUser);

        // Header metrics matching the design
        model.addAttribute("locationName", "TP. Hồ Chí Minh");
        model.addAttribute("compareCount", 2);
        model.addAttribute("cartCount", 3);
        model.addAttribute("cartTotal", "68.490.000₫");

        return "home";
    }
}
