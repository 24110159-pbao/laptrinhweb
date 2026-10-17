package example.com.ltw.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Vui lòng nhập họ và tên")
    @Size(max = 150, message = "Họ và tên tối đa 150 ký tự")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập địa chỉ email")
    @Email(message = "Định dạng email không hợp lệ")
    private String email;

    @Size(max = 20, message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    private String confirmPassword;
}
