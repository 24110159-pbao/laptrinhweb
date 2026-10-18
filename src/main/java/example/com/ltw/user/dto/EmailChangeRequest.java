package example.com.ltw.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailChangeRequest {
    @NotBlank(message = "Vui lòng nhập email mới.")
    @Email(message = "Email mới không đúng định dạng.")
    @Size(max = 150, message = "Email tối đa 150 ký tự.")
    private String email;
}
