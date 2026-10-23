package example.com.ltw.catalog.dto;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public record ProductVariantDto(
        Long id,
        String ram,
        String storage,
        String color,
        BigDecimal price,
        BigDecimal salePrice,
        String imageUrl,
        int totalStock
) {
    public BigDecimal getEffectivePrice() {
        return salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0
                && salePrice.compareTo(price) < 0
                ? salePrice : price;
    }

    public String getFormattedEffectivePrice() {
        return NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(getEffectivePrice()) + "₫";
    }

    public String getFormattedOriginalPrice() {
        return salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0
                && salePrice.compareTo(price) < 0
                ? NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(price) + "₫" : null;
    }

    public boolean isInStock() {
        return totalStock > 0;
    }
}
