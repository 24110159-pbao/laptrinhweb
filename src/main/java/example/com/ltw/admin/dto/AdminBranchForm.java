package example.com.ltw.admin.dto;

import example.com.ltw.branch.entity.BranchStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AdminBranchForm {

    private Long id;

    @NotBlank(message = "Tên chi nhánh không được để trống.")
    @Size(max = 150, message = "Tên chi nhánh tối đa 150 ký tự.")
    private String name;

    @NotBlank(message = "Địa chỉ không được để trống.")
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự.")
    private String address;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự.")
    private String phone;

    @Email(message = "Email không đúng định dạng.")
    @Size(max = 150, message = "Email tối đa 150 ký tự.")
    private String email;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private BigDecimal latitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private BigDecimal longitude;

    private BranchStatus status = BranchStatus.ACTIVE;
}
