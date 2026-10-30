package example.com.ltw.cart.dto;

import java.math.BigDecimal;

public record CartLineView(
        Long itemId,
        String productName,
        String variantDescription,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        int availableQuantity
) {
}
