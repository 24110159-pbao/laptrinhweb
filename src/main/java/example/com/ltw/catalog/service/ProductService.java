package example.com.ltw.catalog.service;

import example.com.ltw.catalog.dto.ProductCardDto;
import example.com.ltw.catalog.entity.Product;
import example.com.ltw.catalog.entity.ProductStatus;
import example.com.ltw.catalog.entity.ProductVariant;
import example.com.ltw.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductCardDto> getFeaturedProducts(String brand, String keyword) {
        List<Product> products;

        if (keyword != null && !keyword.trim().isEmpty()) {
            products = productRepository.searchProducts(ProductStatus.ACTIVE, keyword.trim());
        } else if (brand != null && !brand.equalsIgnoreCase("all") && !brand.trim().isEmpty()) {
            products = productRepository.findByStatusAndBrandName(ProductStatus.ACTIVE, brand.trim());
        } else {
            products = productRepository.findByStatus(ProductStatus.ACTIVE);
        }

        return products.stream().map(this::mapToCardDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ProductCardDto> getProductBySlug(String slug) {
        return productRepository.findBySlugAndStatus(slug, ProductStatus.ACTIVE).map(this::mapToCardDto);
    }

    public ProductCardDto mapToCardDto(Product product) {
        BigDecimal price = BigDecimal.ZERO;
        BigDecimal originalPrice = null;
        String specsLine = "";
        String imageUrl = "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=500&q=80";

        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            ProductVariant variant = product.getVariants().get(0);
            if (variant.getSalePrice() != null && variant.getSalePrice().compareTo(BigDecimal.ZERO) > 0) {
                price = variant.getSalePrice();
                originalPrice = variant.getPrice();
            } else {
                price = variant.getPrice();
            }
        }

        if (product.getDescription() != null && product.getDescription().contains("•")) {
            specsLine = product.getDescription();
        } else if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            ProductVariant variant = product.getVariants().get(0);
            StringBuilder sb = new StringBuilder();
            if (product.getChipset() != null && !product.getChipset().isBlank()) {
                sb.append(product.getChipset()).append(" • ");
            }
            if (variant.getStorage() != null && !variant.getStorage().isBlank()) {
                sb.append(variant.getStorage()).append(" • ");
            }
            sb.append("Chính Hãng VN/A");
            specsLine = sb.toString();
        } else {
            specsLine = (product.getChipset() != null ? product.getChipset() + " • " : "") + "Flagship Đỉnh Cao";
        }

        if (product.getImages() != null && !product.getImages().isEmpty()) {
            imageUrl = product.getImages().get(0).getImageUrl();
        }

        return ProductCardDto.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand() != null ? product.getBrand().getName() : "Khác")
                .category(product.getCategory() != null ? product.getCategory().getName() : "Điện thoại")
                .specsLine(specsLine)
                .price(price)
                .originalPrice(originalPrice)
                .imageUrl(imageUrl)
                .slug(product.getSlug())
                .build();
    }
}
