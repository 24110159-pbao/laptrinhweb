package example.com.ltw.cart.dto;

public record CartOption(
        Long inventoryId,
        String label,
        int availableQuantity
) {
}
