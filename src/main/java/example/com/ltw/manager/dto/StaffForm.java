package example.com.ltw.manager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffForm {

    @NotBlank(message = "Vui lòng nhập họ tên.")
    @Size(max = 150, message = "Họ tên không được vượt quá 150 ký tự.")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Email không hợp lệ.")
    @Size(max = 150, message = "Email không được vượt quá 150 ký tự.")
    private String email;

    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự.")
    private String phone;

    @Pattern(regexp = "^$|.{8,72}$", message = "Mật khẩu phải có từ 8 đến 72 ký tự.")
    private String password;
}
