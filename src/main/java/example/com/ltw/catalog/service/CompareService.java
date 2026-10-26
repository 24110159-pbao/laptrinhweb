package example.com.ltw.catalog.service;

import example.com.ltw.branch.entity.BranchStatus;
import example.com.ltw.catalog.dto.CategoryCompareDto;
import example.com.ltw.catalog.dto.ComparisonResultDto;
import example.com.ltw.catalog.dto.ProductCompareDto;
import example.com.ltw.catalog.entity.*;
import example.com.ltw.catalog.repository.CategoryRepository;
import example.com.ltw.catalog.repository.ProductRepository;
import example.com.ltw.inventory.repository.BranchInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompareService {

    private static final String DEFAULT_IMAGE =
            "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=500&q=80";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BranchInventoryRepository inventoryRepository;

    /**
     * Lấy danh sách các danh mục có sản phẩm đang hoạt động để người dùng chọn so sánh (chỉ hỗ trợ Điện thoại).
     */
    @Transactional(readOnly = true)
    public List<CategoryCompareDto> getCompareCategories() {
        List<Category> categories = categoryRepository.findAll().stream()
                .filter(c -> c.getStatus() == CategoryStatus.ACTIVE && isPhoneCategory(c))
                .toList();

        List<Product> activeProducts = productRepository.findByStatus(ProductStatus.ACTIVE);
        Map<Long, Long> countsByCategory = activeProducts.stream()
                .filter(p -> p.getCategory() != null)
                .collect(Collectors.groupingBy(p -> p.getCategory().getId(), Collectors.counting()));

        return categories.stream()
                .map(c -> CategoryCompareDto.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .slug(c.getSlug())
                        .productCount(countsByCategory.getOrDefault(c.getId(), 0L))
                        .build())
                .filter(c -> c.getProductCount() > 0)
                .sorted(Comparator.comparingLong(CategoryCompareDto::getProductCount).reversed()
                        .thenComparing(CategoryCompareDto::getName))
                .toList();
    }

    private boolean isPhoneCategory(Category c) {
        if (c == null) return false;
        String slug = c.getSlug() != null ? c.getSlug().toLowerCase() : "";
        String name = c.getName() != null ? c.getName().toLowerCase() : "";
        return slug.contains("dien-thoai") || slug.contains("phone")
                || name.contains("điện thoại") || name.contains("phone")
                || (c.getId() != null && c.getId() == 1L);
    }

    /**
     * Lấy tất cả sản phẩm của danh mục điện thoại để người dùng chọn vào Slot so sánh.
     */
    @Transactional(readOnly = true)
    public List<ProductCompareDto> getProductsByCategory(Long categoryId) {
        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId).orElse(null);
        }
        if (category == null || !isPhoneCategory(category)) {
            category = categoryRepository.findAll().stream()
                    .filter(c -> c.getStatus() == CategoryStatus.ACTIVE && isPhoneCategory(c))
                    .findFirst()
                    .orElse(null);
        }
        if (category == null) return Collections.emptyList();

        Long targetCatId = category.getId();
        List<Product> products = productRepository.findByStatus(ProductStatus.ACTIVE).stream()
                .filter(p -> p.getCategory() != null && targetCatId.equals(p.getCategory().getId()))
                .sorted(Comparator.comparing(Product::getName))
                .toList();

        Map<Long, Integer> stocks = getVariantStockMap();

        return products.stream()
                .map(p -> mapToCompareDto(p, stocks))
                .toList();
    }

    /**
     * Lấy thông số chi tiết của một sản phẩm để so sánh.
     */
    @Transactional(readOnly = true)
    public ProductCompareDto getProductCompare(Long productId) {
        if (productId == null) return null;
        Product product = productRepository.findByIdAndStatus(productId, ProductStatus.ACTIVE)
                .orElse(null);
        if (product == null) return null;

        Map<Long, Integer> stocks = getVariantStockMap();
        return mapToCompareDto(product, stocks);
    }

    /**
     * Thực hiện đối chiếu so sánh 2 sản phẩm CÙNG LOẠI (cùng categoryId).
     */
    @Transactional(readOnly = true)
    public ComparisonResultDto compareTwoProducts(Long id1, Long id2) {
        if (id1 == null || id2 == null) {
            throw new IllegalArgumentException("Vui lòng chọn đủ 2 sản phẩm để so sánh!");
        }

        ProductCompareDto p1 = getProductCompare(id1);
        ProductCompareDto p2 = getProductCompare(id2);

        if (p1 == null || p2 == null) {
            throw new IllegalArgumentException("Không tìm thấy thông tin của một trong 2 sản phẩm cần so sánh!");
        }

        // BẮT BUỘC: Kiểm tra 2 sản phẩm phải CÙNG LOẠI (cùng danh mục)
        if (!Objects.equals(p1.getCategoryId(), p2.getCategoryId())) {
            throw new IllegalArgumentException(String.format(
                    "Chỉ có thể so sánh 2 sản phẩm cùng loại! '%s' thuộc nhóm '%s', còn '%s' thuộc nhóm '%s'.",
                    p1.getName(), p1.getCategoryName(), p2.getName(), p2.getCategoryName()
            ));
        }

        // Đánh giá màn hình (lớn hơn là tốt hơn)
        int screenWinner = 0;
        if (p1.getScreenSize() != null && p2.getScreenSize() != null) {
            int cmp = p1.getScreenSize().compareTo(p2.getScreenSize());
            if (cmp > 0) screenWinner = 1;
            else if (cmp < 0) screenWinner = 2;
        } else if (p1.getScreenSize() != null) {
            screenWinner = 1;
        } else if (p2.getScreenSize() != null) {
            screenWinner = 2;
        }

        // Đánh giá pin (dung lượng lớn hơn)
        int batteryWinner = 0;
        int b1 = p1.getBatteryCapacity() != null ? p1.getBatteryCapacity() : 0;
        int b2 = p2.getBatteryCapacity() != null ? p2.getBatteryCapacity() : 0;
        if (b1 > b2) batteryWinner = 1;
        else if (b2 > b1) batteryWinner = 2;

        // Đánh giá giá bán (giá thấp hơn là tiết kiệm hơn)
        int priceWinner = 0;
        if (p1.getPrice() != null && p2.getPrice() != null) {
            int cmp = p1.getPrice().compareTo(p2.getPrice());
            if (cmp < 0) priceWinner = 1; // p1 rẻ hơn
            else if (cmp > 0) priceWinner = 2; // p2 rẻ hơn
        }

        return ComparisonResultDto.builder()
                .categoryId(p1.getCategoryId())
                .categoryName(p1.getCategoryName())
                .product1(p1)
                .product2(p2)
                .screenSizeWinner(screenWinner)
                .batteryWinner(batteryWinner)
                .priceWinner(priceWinner)
                .build();
    }

    private ProductCompareDto mapToCompareDto(Product product, Map<Long, Integer> stocks) {
        List<ProductVariant> variants = product.getVariants() == null ? List.of() : product.getVariants().stream()
                .filter(v -> v.getStatus() == ProductVariantStatus.ACTIVE)
                .sorted(Comparator.comparing(ProductVariant::getId))
                .toList();

        ProductVariant cheapest = variants.stream()
                .min(Comparator.comparing(this::effectivePrice).thenComparing(ProductVariant::getId))
                .orElse(null);

        BigDecimal price = cheapest == null ? null : effectivePrice(cheapest);
        BigDecimal originalPrice = (cheapest != null && cheapest.getSalePrice() != null
                && cheapest.getSalePrice().compareTo(BigDecimal.ZERO) > 0
                && cheapest.getPrice().compareTo(price) > 0) ? cheapest.getPrice() : null;

        Integer discountPercent = null;
        if (originalPrice != null && price != null && originalPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = originalPrice.subtract(price);
            discountPercent = diff.divide(originalPrice, 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).intValue();
        }

        int totalStock = variants.stream()
                .mapToInt(v -> stocks.getOrDefault(v.getId(), 0))
                .sum();

        List<String> rams = variants.stream()
                .map(ProductVariant::getRam)
                .filter(r -> r != null && !r.isBlank())
                .distinct()
                .toList();

        List<String> storages = variants.stream()
                .map(ProductVariant::getStorage)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .toList();

        List<String> colors = variants.stream()
                .map(ProductVariant::getColor)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .toList();

        List<String> imageUrls = product.getImages() == null ? List.of() : product.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::isThumbnail).reversed()
                        .thenComparing(ProductImage::getDisplayOrder)
                        .thenComparing(ProductImage::getId))
                .map(ProductImage::getImageUrl)
                .toList();
        String imageUrl = imageUrls.isEmpty() ? DEFAULT_IMAGE : imageUrls.get(0);

        String specsLine = "";
        if (product.getDescription() != null && product.getDescription().contains("•")) {
            specsLine = product.getDescription();
        } else if (!variants.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if (product.getChipset() != null && !product.getChipset().isBlank()) {
                sb.append(product.getChipset()).append(" • ");
            }
            sb.append(variants.get(0).getStorage()).append(" • Chính Hãng");
            specsLine = sb.toString();
        } else if (product.getChipset() != null) {
            specsLine = product.getChipset();
        }

        return ProductCompareDto.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : "Khác")
                .brandName(product.getBrand() != null ? product.getBrand().getName() : "Khác")
                .imageUrl(imageUrl)
                .price(price)
                .originalPrice(originalPrice)
                .discountPercent(discountPercent)
                .totalStock(totalStock)
                .inStock(totalStock > 0)
                .specsLine(specsLine)
                .operatingSystem(product.getOperatingSystem())
                .chipset(product.getChipset())
                .screenSize(product.getScreenSize())
                .batteryCapacity(product.getBatteryCapacity())
                .ramOptions(rams)
                .storageOptions(storages)
                .colorOptions(colors)
                .description(product.getDescription())
                .build();
    }

    private BigDecimal effectivePrice(ProductVariant variant) {
        return variant.getSalePrice() != null
                && variant.getSalePrice().compareTo(BigDecimal.ZERO) > 0
                && variant.getSalePrice().compareTo(variant.getPrice()) < 0
                ? variant.getSalePrice()
                : variant.getPrice();
    }

    private Map<Long, Integer> getVariantStockMap() {
        Map<Long, Integer> stocks = new HashMap<>();
        try {
            inventoryRepository.findCatalogInventory(
                            BranchStatus.ACTIVE, ProductVariantStatus.ACTIVE, ProductStatus.ACTIVE)
                    .forEach(inv -> stocks.merge(inv.getVariant().getId(), inv.getAvailableQuantity(), Integer::sum));
        } catch (Exception e) {
            log.warn("Không thể tải tồn kho catalog cho chức năng so sánh: {}", e.getMessage());
        }
        return stocks;
    }
}
