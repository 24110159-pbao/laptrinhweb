package example.com.ltw.admin.dto;

import example.com.ltw.user.entity.Role;
import example.com.ltw.user.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserForm {

    private Long id;

    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(max = 150, message = "Họ tên không vượt quá 150 ký tự.")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Email không đúng định dạng.")
    @Size(max = 150, message = "Email không vượt quá 150 ký tự.")
    private String email;

    @Pattern(
            regexp = "^(0[0-9]{9})?$",
            message = "Số điện thoại phải gồm 10 chữ số bắt đầu bằng số 0."
    )
    private String phone;

    @Size(max = 100, message = "Mật khẩu tối đa 100 ký tự.")
    private String password;

    private Role role = Role.MANAGER;

    private Long branchId;

    private UserStatus status = UserStatus.ACTIVE;
}
