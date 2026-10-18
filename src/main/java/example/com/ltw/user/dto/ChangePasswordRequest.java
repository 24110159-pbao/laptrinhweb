package example.com.ltw.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {
    @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại.")
    private String currentPassword;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới.")
    @Size(min = 6, max = 72, message = "Mật khẩu mới phải có từ 6 đến 72 ký tự.")
    private String newPassword;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu mới.")
    private String confirmPassword;
}
