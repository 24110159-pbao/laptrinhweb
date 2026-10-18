package example.com.ltw.user.dto;

import example.com.ltw.user.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProfileUpdateRequest {
    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(max = 150, message = "Họ và tên tối đa 150 ký tự.")
    private String fullName;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự.")
    @Pattern(regexp = "^$|^(?:\\+?84|0)[1-9][0-9]{8}$", message = "Số điện thoại không đúng định dạng Việt Nam.")
    private String phone;

    @Past(message = "Ngày sinh phải nhỏ hơn ngày hiện tại.")
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private LocalDate dateOfBirth;

    private Gender gender;
}
