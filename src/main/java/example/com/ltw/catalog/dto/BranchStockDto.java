package example.com.ltw.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

public record BranchStockDto(
        String name,
        String address,
        String phone,
        BigDecimal latitude,
        BigDecimal longitude,
        List<VariantStock> variants
) {
    public record VariantStock(Long variantId, String label, Long inventoryId, int quantity) {
    }
}
