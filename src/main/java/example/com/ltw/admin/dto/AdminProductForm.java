package example.com.ltw.admin.dto;

import example.com.ltw.catalog.entity.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AdminProductForm {

    private Long id;

    @NotBlank(message = "Tên sản phẩm không được để trống.")
    @Size(max = 200, message = "Tên sản phẩm tối đa 200 ký tự.")
    private String name;

    @Size(max = 220, message = "Slug tối đa 220 ký tự.")
    private String slug;

    private String description;

    @Size(max = 100, message = "Hệ điều hành tối đa 100 ký tự.")
    private String operatingSystem;

    @Size(max = 150, message = "Chipset tối đa 150 ký tự.")
    private String chipset;

    @DecimalMin(value = "0.0", message = "Kích thước màn hình phải lớn hơn hoặc bằng 0.")
    private BigDecimal screenSize;

    private Integer batteryCapacity;

    @NotNull(message = "Vui lòng chọn thương hiệu.")
    private Long brandId;

    @NotNull(message = "Vui lòng chọn danh mục.")
    private Long categoryId;

    private ProductStatus status = ProductStatus.ACTIVE;
}
