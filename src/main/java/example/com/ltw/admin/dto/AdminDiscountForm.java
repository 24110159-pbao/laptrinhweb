package example.com.ltw.admin.dto;

import example.com.ltw.discount.entity.DiscountStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class AdminDiscountForm {

    private Long id;

    @NotBlank(message = "Mã giảm giá không được để trống.")
    @Size(max = 50, message = "Mã giảm giá tối đa 50 ký tự.")
    private String code;

    @NotNull(message = "Giá trị giảm không được để trống.")
    @DecimalMin(value = "0.0", message = "Tỷ lệ giảm phải >= 0%.")
    @DecimalMax(value = "100.0", message = "Tỷ lệ giảm tối đa 100%.")
    private BigDecimal discountValue;

    @NotNull(message = "Số lượng không được để trống.")
    @PositiveOrZero(message = "Số lượng mã phải >= 0.")
    private Integer quantity;

    @NotNull(message = "Thời gian bắt đầu không được để trống.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startAt;

    @NotNull(message = "Thời gian kết thúc không được để trống.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endAt;

    private DiscountStatus status = DiscountStatus.ACTIVE;
}
