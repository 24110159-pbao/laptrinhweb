package example.com.ltw.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderCancelRequest {

    @NotBlank(message = "Vui lòng nhập lý do hủy đơn hàng!")
    @Size(max = 500, message = "Lý do hủy không được vượt quá 500 ký tự!")
    private String reason;
}
