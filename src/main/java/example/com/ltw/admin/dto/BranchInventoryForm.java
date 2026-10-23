package example.com.ltw.admin.dto;

import example.com.ltw.catalog.entity.ProductVariantStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BranchInventoryForm {

    @NotNull(message = "Vui lòng chọn chi nhánh.")
    private Long branchId;

    private Long productId;

    @NotNull(message = "Vui lòng chọn biến thể sản phẩm.")
    private Long variantId;

    @NotNull(message = "Vui lòng nhập số lượng tồn kho.")
    @PositiveOrZero(message = "Số lượng tồn kho phải >= 0.")
    private Integer quantity;

    private ProductVariantStatus status = ProductVariantStatus.ACTIVE;
}
