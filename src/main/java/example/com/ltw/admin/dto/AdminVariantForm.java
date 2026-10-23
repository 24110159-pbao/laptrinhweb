package example.com.ltw.admin.dto;

import example.com.ltw.catalog.entity.ProductVariantStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AdminVariantForm {

    private Long id;

    private Long productId;

    @NotBlank(message = "RAM không được để trống.")
    @Size(max = 30, message = "RAM tối đa 30 ký tự.")
    private String ram;

    @NotBlank(message = "Dung lượng bộ nhớ không được để trống.")
    @Size(max = 30, message = "Bộ nhớ tối đa 30 ký tự.")
    private String storage;

    @NotBlank(message = "Màu sắc không được để trống.")
    @Size(max = 50, message = "Màu sắc tối đa 50 ký tự.")
    private String color;

    @NotNull(message = "Giá gốc không được để trống.")
    @DecimalMin(value = "0.0", message = "Giá gốc phải >= 0.")
    private BigDecimal price;

    @DecimalMin(value = "0.0", message = "Giá khuyến mãi phải >= 0.")
    private BigDecimal salePrice;

    private ProductVariantStatus status = ProductVariantStatus.ACTIVE;
}
