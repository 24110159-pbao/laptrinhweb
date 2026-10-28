package example.com.ltw.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import example.com.ltw.catalog.entity.CategoryStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCategoryForm {

    private Long id;

    @NotBlank(message = "Tên danh mục không được để trống.")
    @Size(max = 150, message = "Tên danh mục tối đa 150 ký tự.")
    private String name;

    @Size(max = 180, message = "Slug tối đa 180 ký tự.")
    private String slug;

    private String description;

    private CategoryStatus status = CategoryStatus.ACTIVE;
}