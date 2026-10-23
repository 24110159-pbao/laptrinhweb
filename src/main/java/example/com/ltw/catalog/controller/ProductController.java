package example.com.ltw.catalog.controller;

import example.com.ltw.catalog.dto.ProductCardDto;
import example.com.ltw.catalog.entity.BrandStatus;
import example.com.ltw.catalog.entity.CategoryStatus;
import example.com.ltw.catalog.repository.BrandRepository;
import example.com.ltw.catalog.repository.CategoryRepository;
import example.com.ltw.catalog.service.ProductService;
import example.com.ltw.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ReviewService reviewService;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping("/products")
    public String list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) List<String> categories,
            @RequestParam(value = "brand", required = false) List<String> brands,
            @RequestParam(value = "minPrice", required = false) String minimumPrice,
            @RequestParam(value = "maxPrice", required = false) String maximumPrice,
            @RequestParam(value = "stock", required = false) String stock,
            @RequestParam(value = "sort", required = false, defaultValue = "newest") String requestedSort,
            @RequestParam(value = "page", required = false, defaultValue = "1") String requestedPage,
            Model model
    ) {
        Set<Long> requestedCategoryIds = Set.copyOf(parseIds(categories));
        Set<Long> requestedBrandIds = Set.copyOf(parseIds(brands));
        List<Long> validCategories = categoryRepository.findAll().stream()
                .filter(category -> category.getStatus() == CategoryStatus.ACTIVE)
                .map(category -> category.getId())
                .filter(requestedCategoryIds::contains)
                .toList();
        List<Long> validBrands = brandRepository.findAll().stream()
                .filter(brand -> brand.getStatus() == BrandStatus.ACTIVE)
                .map(brand -> brand.getId())
                .filter(requestedBrandIds::contains)
                .toList();
        BigDecimal min = parsePrice(minimumPrice);
        BigDecimal max = parsePrice(maximumPrice);
        if (min != null && max != null && min.compareTo(max) > 0) {
            min = null;
            max = null;
        }
        String sort = Set.of("newest", "price_asc", "price_desc", "best_selling")
                .contains(requestedSort) ? requestedSort : "newest";
        String stockFilter = "in".equals(stock) || "out".equals(stock) ? stock : "";
        int pageNumber = parsePage(requestedPage);

        var products = productService.searchProducts(
                keyword, validCategories, validBrands, min, max, stockFilter, sort, pageNumber);
        model.addAttribute("products", products.getContent());
        model.addAttribute("categories", categoryRepository.findAll().stream()
                .filter(category -> category.getStatus() == CategoryStatus.ACTIVE).toList());
        model.addAttribute("brands", brandRepository.findAll().stream()
                .filter(brand -> brand.getStatus() == BrandStatus.ACTIVE).toList());
        model.addAttribute("selectedCategories", validCategories);
        model.addAttribute("selectedBrands", validBrands);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("minPrice", min == null ? "" : min.toPlainString());
        model.addAttribute("maxPrice", max == null ? "" : max.toPlainString());
        model.addAttribute("stock", stockFilter);
        model.addAttribute("sort", sort);
        model.addAttribute("pageNumber", products.getNumber() + 1);
        model.addAttribute("totalPages", Math.max(1, products.getTotalPages()));
        model.addAttribute("totalElements", products.getTotalElements());
        UriComponentsBuilder returnUrl = UriComponentsBuilder.fromPath("/products");
        if (keyword != null && !keyword.isBlank()) returnUrl.queryParam("keyword", keyword);
        if (!validCategories.isEmpty()) returnUrl.queryParam("category", validCategories);
        if (!validBrands.isEmpty()) returnUrl.queryParam("brand", validBrands);
        if (min != null) returnUrl.queryParam("minPrice", min.toPlainString());
        if (max != null) returnUrl.queryParam("maxPrice", max.toPlainString());
        if (!stockFilter.isEmpty()) returnUrl.queryParam("stock", stockFilter);
        if (!"newest".equals(sort)) returnUrl.queryParam("sort", sort);
        if (products.getNumber() > 0) returnUrl.queryParam("page", products.getNumber() + 1);
        model.addAttribute("catalogReturnUrl", returnUrl.build().encode().toUriString());
        model.addAttribute("suggestions", productService.getNewestSuggestions());
        return "catalog/list";
    }

    @GetMapping("/product/{slug}")
    public String detail(
            @PathVariable String slug,
            @RequestParam(value = "returnTo", required = false) String requestedReturnTo,
            Model model,
            HttpServletResponse response
    ) {
        ProductCardDto product = productService.getProductDetailsBySlug(slug).orElse(null);
        if (product == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            model.addAttribute("suggestions", productService.getNewestSuggestions());
            return "catalog/product-not-found";
        }
        model.addAttribute("product", product);
        model.addAttribute("returnTo", validCatalogReturnUrl(requestedReturnTo));
        model.addAttribute("reviewStats", reviewService.getProductStatistics(product.getId()));
        model.addAttribute("reviews", reviewService.getProductReviews(product.getId()));
        return "catalog/product";
    }

    private String validCatalogReturnUrl(String requestedReturnTo) {
        if (requestedReturnTo == null || requestedReturnTo.isBlank()) {
            return "/products";
        }
        try {
            URI uri = URI.create(requestedReturnTo);
            if (!uri.isAbsolute() && uri.getRawAuthority() == null
                    && "/products".equals(uri.getRawPath()) && requestedReturnTo.startsWith("/")) {
                return uri.toASCIIString();
            }
        } catch (IllegalArgumentException ignored) {
            return "/products";
        }
        return "/products";
    }

    private List<Long> parseIds(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().map(value -> {
                    try {
                        long id = Long.parseLong(value);
                        return id > 0 ? id : null;
                    } catch (NumberFormatException exception) {
                        return null;
                    }
                })
                .filter(id -> id != null)
                .distinct()
                .toList();
    }

    private BigDecimal parsePrice(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            BigDecimal price = new BigDecimal(value);
            return price.signum() >= 0 ? price : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private int parsePage(String value) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }
}
