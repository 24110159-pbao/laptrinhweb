package example.com.ltw.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartView(
        Long cartId,
        Long branchId,
        String branchName,
        List<CartLineView> lines,
        BigDecimal subtotal
) {
    public CartView(Long cartId, String branchName, List<CartLineView> lines, BigDecimal subtotal) {
        this(cartId, null, branchName, lines, subtotal);
    }
}
