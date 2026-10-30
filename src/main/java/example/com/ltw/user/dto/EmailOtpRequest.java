package example.com.ltw.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailOtpRequest {
    @NotBlank(message = "Vui lòng nhập mã OTP.")
    @Pattern(regexp = "^[0-9]{6}$", message = "Mã OTP gồm đúng 6 chữ số.")
    private String otp;
}
