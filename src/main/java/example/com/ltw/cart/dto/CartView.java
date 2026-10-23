package example.com.ltw.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartView(
        Long cartId,
        String branchName,
        List<CartLineView> lines,
        BigDecimal subtotal
) {
}
