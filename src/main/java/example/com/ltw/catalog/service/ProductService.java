package example.com.ltw.catalog.service;

import example.com.ltw.branch.entity.Branch;
import example.com.ltw.branch.entity.BranchStatus;
import example.com.ltw.branch.repository.BranchRepository;
import example.com.ltw.catalog.dto.BranchStockDto;
import example.com.ltw.catalog.dto.ProductCardDto;
import example.com.ltw.catalog.dto.ProductVariantDto;
import example.com.ltw.catalog.entity.Product;
import example.com.ltw.catalog.entity.ProductImage;
import example.com.ltw.catalog.entity.ProductStatus;
import example.com.ltw.catalog.entity.ProductVariant;
import example.com.ltw.catalog.entity.ProductVariantStatus;
import example.com.ltw.catalog.repository.ProductRepository;
import example.com.ltw.inventory.entity.BranchInventory;
import example.com.ltw.inventory.repository.BranchInventoryRepository;
import example.com.ltw.order.entity.OrderStatus;
import example.com.ltw.order.repository.OrderItemCatalogProjection;
import example.com.ltw.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int PAGE_SIZE = 10;
    private static final int HOME_PRODUCT_LIMIT = 8;
    private static final String DEFAULT_IMAGE =
            "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=500&q=80";

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final BranchInventoryRepository inventoryRepository;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public List<ProductCardDto> getNewestProducts() {
        return catalog().stream()
                .sorted(newestFirst())
                .limit(HOME_PRODUCT_LIMIT)
                .map(CatalogEntry::card)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductCardDto> getBestSellingProducts() {
        return catalog().stream()
                .sorted(Comparator.comparingLong((CatalogEntry entry) -> entry.card().getQuantitySold()).reversed()
                        .thenComparing(entry -> entry.product().getId()))
                .limit(HOME_PRODUCT_LIMIT)
                .map(CatalogEntry::card)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductCardDto> searchProducts(
            String keyword,
            List<Long> categoryIds,
            List<Long> brandIds,
            BigDecimal minimumPrice,
            BigDecimal maximumPrice,
            String stock,
            String sort,
            int requestedPage
    ) {
        List<CatalogEntry> matches = catalog().stream()
                .filter(entry -> matchesKeyword(entry.product(), keyword))
                .filter(entry -> categoryIds.isEmpty() || categoryIds.contains(entry.product().getCategory().getId()))
                .filter(entry -> brandIds.isEmpty() || brandIds.contains(entry.product().getBrand().getId()))
                .filter(entry -> matchesPrice(entry.card().getPrice(), minimumPrice, maximumPrice))
                .filter(entry -> matchesStock(entry.card().getTotalStock(), stock))
                .sorted(comparatorFor(sort))
                .toList();

        int totalPages = Math.max(1, (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(Math.max(requestedPage, 1), totalPages);
        int fromIndex = Math.min((page - 1) * PAGE_SIZE, matches.size());
        int toIndex = Math.min(fromIndex + PAGE_SIZE, matches.size());
        List<ProductCardDto> content = matches.subList(fromIndex, toIndex).stream()
                .map(CatalogEntry::card)
                .toList();
        return new PageImpl<>(content, PageRequest.of(page - 1, PAGE_SIZE), matches.size());
    }

    @Transactional(readOnly = true)
    public java.util.Optional<ProductCardDto> getProductBySlug(String slug) {
        return productRepository.findBySlugAndStatus(slug, ProductStatus.ACTIVE)
                .map(this::mapToCardDto);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<ProductCardDto> getProductDetailsBySlug(String slug) {
        return productRepository.findBySlugAndStatus(slug, ProductStatus.ACTIVE).map(product -> {
            ProductCardDto card = mapToCardDto(product);
            card.setBrand(product.getBrand() == null ? null : product.getBrand().getName());
            card.setCategory(product.getCategory() == null ? null : product.getCategory().getName());
            List<ProductVariant> variants = activeVariants(product);
            List<Branch> branches = branchRepository.findByStatusOrderByNameAsc(BranchStatus.ACTIVE);
            Map<Long, Map<Long, BranchInventory>> inventoryByBranchAndVariant = new HashMap<>();
            inventoryRepository.findCatalogInventory(
                            BranchStatus.ACTIVE, ProductVariantStatus.ACTIVE, ProductStatus.ACTIVE)
                    .stream()
                    .filter(inventory -> inventory.getVariant().getProduct().getId().equals(product.getId()))
                    .forEach(inventory -> inventoryByBranchAndVariant
                            .computeIfAbsent(inventory.getBranch().getId(), ignored -> new HashMap<>())
                            .put(inventory.getVariant().getId(), inventory));

            card.setVariants(variants.stream()
                    .map(variant -> new ProductVariantDto(
                            variant.getId(), variant.getRam(), variant.getStorage(), variant.getColor(),
                            variant.getPrice(), variant.getSalePrice(),
                            variant.getImageUrl() == null || variant.getImageUrl().isBlank()
                                    ? null : variant.getImageUrl(),
                            totalAvailableStock(variant.getId(), inventoryByBranchAndVariant)))
                    .toList());
            card.setTotalStock(card.getVariants().stream()
                    .mapToInt(ProductVariantDto::totalStock).sum());
            card.setBranchStocks(branches.stream().map(branch -> new BranchStockDto(
                    branch.getName(),
                    branch.getAddress(),
                    branch.getPhone(),
                    branch.getLatitude(),
                    branch.getLongitude(),
                    variants.stream().map(variant -> new BranchStockDto.VariantStock(
                            variant.getId(),
                            variant.getRam() + " RAM • " + variant.getStorage() + " • " + variant.getColor(),
                            inventoryByBranchAndVariant.getOrDefault(branch.getId(), Map.of())
                                    .get(variant.getId()) == null ? null
                                    : inventoryByBranchAndVariant.get(branch.getId()).get(variant.getId()).getId(),
                            availableQuantity(branch.getId(), variant.getId(), inventoryByBranchAndVariant)))
                            .toList()
            )).toList());
            if (!card.getVariants().isEmpty()) {
                ProductVariantDto defaultVariant = card.getVariants().get(0);
                card.setPrice(defaultVariant.getEffectivePrice());
                card.setOriginalPrice(defaultVariant.getFormattedOriginalPrice() == null
                        ? null : defaultVariant.price());
            }
            return card;
        });
    }

    @Transactional(readOnly = true)
    public List<ProductCardDto> getNewestSuggestions() {
        return getNewestProducts();
    }

    private List<CatalogEntry> catalog() {
        List<Product> products = productRepository.findByStatus(ProductStatus.ACTIVE);
        Map<Long, Long> sales = orderItemRepository.findSalesByProduct(OrderStatus.DELIVERED).stream()
                .collect(Collectors.toMap(
                        OrderItemCatalogProjection::getProductId,
                        projection -> projection.getQuantitySold() == null ? 0L : projection.getQuantitySold()
                ));
        Map<Long, Integer> stocks = new HashMap<>();
        inventoryRepository.findCatalogInventory(
                        BranchStatus.ACTIVE, ProductVariantStatus.ACTIVE, ProductStatus.ACTIVE)
                .forEach(inventory -> stocks.merge(
                        inventory.getVariant().getId(), inventory.getAvailableQuantity(), Integer::sum));

        return products.stream().map(product -> {
            List<ProductVariant> variants = activeVariants(product);
            int totalStock = variants.stream().mapToInt(variant -> stocks.getOrDefault(variant.getId(), 0)).sum();
            ProductCardDto card = mapToCardDto(product);
            card.setTotalStock(totalStock);
            card.setQuantitySold(sales.getOrDefault(product.getId(), 0L));
            return new CatalogEntry(product, card);
        }).toList();
    }

    public ProductCardDto mapToCardDto(Product product) {
        List<ProductVariant> variants = activeVariants(product);
        ProductVariant cheapest = variants.stream()
                .min(Comparator.comparing(this::effectivePrice)
                        .thenComparing(ProductVariant::getId))
                .orElse(null);
        BigDecimal price = cheapest == null ? null : effectivePrice(cheapest);
        BigDecimal originalPrice = cheapest != null && cheapest.getSalePrice() != null
                && cheapest.getSalePrice().compareTo(BigDecimal.ZERO) > 0
                && cheapest.getPrice().compareTo(price) > 0 ? cheapest.getPrice() : null;

        String specsLine = "";
        if (product.getDescription() != null && product.getDescription().contains("•")) {
            specsLine = product.getDescription();
        } else if (!variants.isEmpty()) {
            StringBuilder specs = new StringBuilder();
            if (product.getChipset() != null && !product.getChipset().isBlank()) {
                specs.append(product.getChipset()).append(" • ");
            }
            specs.append(variants.get(0).getStorage()).append(" • Chính Hãng VN/A");
            specsLine = specs.toString();
        } else if (product.getChipset() != null) {
            specsLine = product.getChipset();
        }

        List<String> imageUrls = product.getImages() == null ? List.of() : product.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::isThumbnail).reversed()
                        .thenComparing(ProductImage::getDisplayOrder)
                        .thenComparing(ProductImage::getId))
                .map(ProductImage::getImageUrl)
                .toList();
        String imageUrl = imageUrls.isEmpty() ? DEFAULT_IMAGE : imageUrls.get(0);
        return ProductCardDto.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand() == null ? "Khác" : product.getBrand().getName())
                .category(product.getCategory() == null ? "Khác" : product.getCategory().getName())
                .categoryId(product.getCategory() == null ? null : product.getCategory().getId())
                .specsLine(specsLine)
                .price(price)
                .originalPrice(originalPrice)
                .imageUrl(imageUrl)
                .slug(product.getSlug())
                .totalStock(0)
                .quantitySold(0L)
                .operatingSystem(product.getOperatingSystem())
                .chipset(product.getChipset())
                .screenSize(product.getScreenSize())
                .batteryCapacity(product.getBatteryCapacity())
                .description(product.getDescription())
                .imageUrls(imageUrls)
                .build();
    }

    private List<ProductVariant> activeVariants(Product product) {
        if (product.getVariants() == null) {
            return List.of();
        }
        return product.getVariants().stream()
                .filter(variant -> variant.getStatus() == ProductVariantStatus.ACTIVE)
                .sorted(Comparator.comparing(ProductVariant::getId))
                .toList();
    }

    private BigDecimal effectivePrice(ProductVariant variant) {
        return variant.getSalePrice() != null
                && variant.getSalePrice().compareTo(BigDecimal.ZERO) > 0
                && variant.getSalePrice().compareTo(variant.getPrice()) < 0
                ? variant.getSalePrice() : variant.getPrice();
    }

    private int totalStock(Long variantId, Map<Long, Map<Long, Integer>> stock) {
        return stock.values().stream().mapToInt(variantStocks -> variantStocks.getOrDefault(variantId, 0)).sum();
    }

    private int totalAvailableStock(Long variantId, Map<Long, Map<Long, BranchInventory>> inventory) {
        return inventory.values().stream()
                .map(branchInventory -> branchInventory.get(variantId))
                .filter(branchInventory -> branchInventory != null)
                .mapToInt(BranchInventory::getAvailableQuantity)
                .sum();
    }

    private int availableQuantity(
            Long branchId, Long variantId, Map<Long, Map<Long, BranchInventory>> inventory
    ) {
        BranchInventory branchInventory = inventory.getOrDefault(branchId, Map.of()).get(variantId);
        return branchInventory == null ? 0 : branchInventory.getAvailableQuantity();
    }

    private boolean matchesKeyword(Product product, String keyword) {
        return keyword == null || keyword.isBlank()
                || normalize(product.getName()).contains(normalize(keyword));
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return normalized.toLowerCase(Locale.ROOT).trim();
    }

    private boolean matchesPrice(BigDecimal price, BigDecimal minimum, BigDecimal maximum) {
        if ((minimum != null || maximum != null) && price == null) {
            return false;
        }
        return (minimum == null || price.compareTo(minimum) >= 0)
                && (maximum == null || price.compareTo(maximum) <= 0);
    }

    private boolean matchesStock(Integer stock, String filter) {
        if ("in".equalsIgnoreCase(filter)) {
            return stock != null && stock > 0;
        }
        if ("out".equalsIgnoreCase(filter)) {
            return stock == null || stock <= 0;
        }
        return true;
    }

    private Comparator<CatalogEntry> newestFirst() {
        return Comparator.comparing((CatalogEntry entry) -> entry.product().getCreatedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(entry -> entry.product().getId());
    }

    private Comparator<CatalogEntry> comparatorFor(String sort) {
        return switch (sort == null ? "newest" : sort) {
            case "price_asc" -> Comparator.comparing(
                    (CatalogEntry entry) -> entry.card().getPrice(),
                    Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(entry -> entry.product().getId());
            case "price_desc" -> Comparator.comparing(
                    (CatalogEntry entry) -> entry.card().getPrice(),
                    Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(entry -> entry.product().getId());
            case "best_selling" -> Comparator.comparingLong(
                    (CatalogEntry entry) -> entry.card().getQuantitySold()).reversed()
                    .thenComparing(entry -> entry.product().getId());
            default -> newestFirst();
        };
    }

    private record CatalogEntry(Product product, ProductCardDto card) {
    }
}
