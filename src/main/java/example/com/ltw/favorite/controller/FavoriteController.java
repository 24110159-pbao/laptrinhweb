package example.com.ltw.favorite.controller;

import example.com.ltw.auth.service.AuthService;
import example.com.ltw.favorite.service.FavoriteService;
import example.com.ltw.user.entity.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping("/favorites")
    public String viewFavorites(HttpSession session, Model model) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        model.addAttribute("favorites", favoriteService.getFavorites(userId));
        model.addAttribute("currentUser", session.getAttribute(AuthService.SESSION_CURRENT_USER));
        return "favorite/index";
    }

    @PostMapping("/favorites")
    public String addFavorite(
            @RequestParam Long productId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            boolean added = favoriteService.addFavorite(userId, productId);
            redirectAttributes.addFlashAttribute(
                    added ? "successMessage" : "infoMessage",
                    added ? "Đã thêm sản phẩm vào danh sách yêu thích."
                            : "Sản phẩm đã có trong danh sách yêu thích."
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/favorites";
    }

    @PostMapping("/favorites/{productId}/remove")
    public String removeFavorite(
            @PathVariable Long productId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            boolean removed = favoriteService.removeFavorite(userId, productId);
            redirectAttributes.addFlashAttribute(
                    removed ? "successMessage" : "infoMessage",
                    removed ? "Đã xóa sản phẩm khỏi danh sách yêu thích."
                            : "Sản phẩm không có trong danh sách yêu thích."
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/favorites";
    }

    private Long currentUserId(HttpSession session) {
        Object currentUser = session.getAttribute(AuthService.SESSION_CURRENT_USER);
        return currentUser instanceof User user ? user.getId() : null;
    }
}
