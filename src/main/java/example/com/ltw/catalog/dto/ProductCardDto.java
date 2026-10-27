package example.com.ltw.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCardDto {

    private Long id;
    private String name;
    private String brand;
    private String category;
    private Long categoryId;
    private String specsLine;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String imageUrl;
    private String slug;
    private Integer totalStock;
    private Long quantitySold;
    private String operatingSystem;
    private String chipset;
    private java.math.BigDecimal screenSize;
    private Integer batteryCapacity;
    private String description;
    private List<ProductVariantDto> variants;
    private List<BranchStockDto> branchStocks;
    private List<String> imageUrls;

    public String getFormattedPrice() {
        if (price == null) return "Liên hệ";
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(price) + "₫";
    }

    public String getFormattedOriginalPrice() {
        if (originalPrice == null) return null;
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(originalPrice) + "₫";
    }

    public Integer getDiscountPercent() {
        if (originalPrice == null || price == null || originalPrice.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        if (originalPrice.compareTo(price) > 0) {
            BigDecimal diff = originalPrice.subtract(price);
            BigDecimal pct = diff.divide(originalPrice, 2, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal(100));
            return pct.intValue();
        }
        return null;
    }
}
