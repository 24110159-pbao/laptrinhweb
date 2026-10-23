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

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Pattern(
            regexp = "^0[0-9]{9}$",
            message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0."
    )
    private String phone;

    @Size(
            min = 6,
            max = 20,
            message = "Mật khẩu phải có từ 6 đến 20 ký tự."
    )
    private String password;
}
