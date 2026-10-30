package example.com.ltw.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import example.com.ltw.catalog.entity.BrandStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminBrandForm {

    private Long id;

    @NotBlank(message = "Tên hãng không được để trống.")
    @Size(max = 100, message = "Tên hãng tối đa 100 ký tự.")
    private String name;

    @Size(max = 1000, message = "URL logo tối đa 1000 ký tự.")
    private String logoUrl;

    private BrandStatus status = BrandStatus.ACTIVE;
}