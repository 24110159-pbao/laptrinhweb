package example.com.ltw.catalog.controller;

import example.com.ltw.catalog.dto.CategoryCompareDto;
import example.com.ltw.catalog.dto.ComparisonResultDto;
import example.com.ltw.catalog.dto.ProductCompareDto;
import example.com.ltw.catalog.service.CompareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import org.springframework.ui.Model;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CompareController {

    private final CompareService compareService;

    /**
     * REST endpoint: Lấy danh sách danh mục có sản phẩm để so sánh.
     */
    @GetMapping("/api/compare/categories")
    @ResponseBody
    public ResponseEntity<List<CategoryCompareDto>> getCategories() {
        return ResponseEntity.ok(compareService.getCompareCategories());
    }

    /**
     * REST endpoint: Lấy danh sách các sản phẩm thuộc danh mục (đảm bảo so sánh cùng loại).
     */
    @GetMapping("/api/compare/category/{categoryId}/products")
    @ResponseBody
    public ResponseEntity<List<ProductCompareDto>> getCategoryProducts(@PathVariable Long categoryId) {
        return ResponseEntity.ok(compareService.getProductsByCategory(categoryId));
    }

    /**
     * REST endpoint: Đối chiếu 2 sản phẩm cụ thể.
     */
    @GetMapping("/api/compare/data")
    @ResponseBody
    public ResponseEntity<?> compareProducts(
            @RequestParam("id1") Long id1,
            @RequestParam("id2") Long id2
    ) {
        try {
            ComparisonResultDto result = compareService.compareTwoProducts(id1, id2);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            log.warn("Lỗi so sánh sản phẩm: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Lỗi hệ thống khi so sánh sản phẩm: {}", ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(Map.of("error", "Đã có lỗi xảy ra khi so sánh sản phẩm."));
        }
    }

    /**
     * Trang so sánh sản phẩm độc lập (/compare).
     */
    @GetMapping("/compare")
    public String comparePage(
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "id1", required = false) Long id1,
            @RequestParam(name = "id2", required = false) Long id2,
            Model model
    ) {
        List<CategoryCompareDto> categories = compareService.getCompareCategories();
        model.addAttribute("categories", categories);

        if (categories.isEmpty()) {
            model.addAttribute("selectedCategory", null);
            model.addAttribute("categoryProducts", List.of());
            model.addAttribute("comparison", null);
            return "catalog/compare";
        }

        CategoryCompareDto activeCategory = null;
        if (id1 != null) {
            ProductCompareDto p1 = compareService.getProductCompare(id1);
            if (p1 != null && p1.getCategoryId() != null) {
                Long catId = p1.getCategoryId();
                for (CategoryCompareDto c : categories) {
                    if (c.getId().equals(catId)) {
                        activeCategory = c;
                        break;
                    }
                }
            }
        }
        if (activeCategory == null && categoryId != null) {
            for (CategoryCompareDto c : categories) {
                if (c.getId().equals(categoryId)) {
                    activeCategory = c;
                    break;
                }
            }
        }
        if (activeCategory == null) {
            activeCategory = categories.get(0);
        }

        List<ProductCompareDto> products = compareService.getProductsByCategory(activeCategory.getId());
        model.addAttribute("selectedCategory", activeCategory);
        model.addAttribute("categoryProducts", products);

        Set<Long> productIds = new HashSet<>();
        for (ProductCompareDto p : products) {
            productIds.add(p.getId());
        }

        Long resolvedProd1Id = (id1 != null && productIds.contains(id1))
                ? id1
                : (!products.isEmpty() ? products.get(0).getId() : null);

        Long resolvedProd2Id = null;
        if (products.size() >= 2) {
            if (id2 != null && !id2.equals(resolvedProd1Id) && productIds.contains(id2)) {
                resolvedProd2Id = id2;
            } else {
                for (ProductCompareDto p : products) {
                    if (!p.getId().equals(resolvedProd1Id)) {
                        resolvedProd2Id = p.getId();
                        break;
                    }
                }
            }
        } else if (products.size() == 1) {
            resolvedProd2Id = resolvedProd1Id;
        }

        if (products.size() >= 2 && resolvedProd1Id != null && resolvedProd2Id != null) {
            try {
                ComparisonResultDto comparison = compareService.compareTwoProducts(resolvedProd1Id, resolvedProd2Id);
                model.addAttribute("comparison", comparison);
            } catch (Exception ex) {
                log.warn("Lỗi đối chiếu sản phẩm: {}", ex.getMessage());
            }
        } else if (products.size() == 1 && resolvedProd1Id != null) {
            try {
                ComparisonResultDto comparison = ComparisonResultDto.builder()
                        .categoryId(activeCategory.getId())
                        .categoryName(activeCategory.getName())
                        .product1(products.get(0))
                        .product2(products.get(0))
                        .build();
                model.addAttribute("comparison", comparison);
            } catch (Exception ignored) {}
        }

        model.addAttribute("product1Id", resolvedProd1Id);
        model.addAttribute("product2Id", resolvedProd2Id);
        model.addAttribute("compareCount", 2);

        return "catalog/compare";
    }
}
