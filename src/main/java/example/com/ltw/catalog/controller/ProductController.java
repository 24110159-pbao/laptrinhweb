package example.com.ltw.catalog.controller;

import example.com.ltw.catalog.dto.ProductCardDto;
import example.com.ltw.catalog.service.ProductService;
import example.com.ltw.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ReviewService reviewService;

    @GetMapping("/product/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        ProductCardDto product = productService.getProductBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("product", product);
        model.addAttribute("reviewStats", reviewService.getProductStatistics(product.getId()));
        model.addAttribute("reviews", reviewService.getProductReviews(product.getId()));
        return "catalog/product";
    }
}
