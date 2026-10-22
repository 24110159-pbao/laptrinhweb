package example.com.ltw.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

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
    private String specsLine;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String imageUrl;
    private String slug;

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
