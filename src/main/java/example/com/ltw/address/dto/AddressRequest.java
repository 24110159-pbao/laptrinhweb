package example.com.ltw.address.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest {
    private Long id;

    @NotBlank(message = "Vui lòng nhập tên người nhận.")
    @Size(max = 150, message = "Tên người nhận tối đa 150 ký tự.")
    private String receiverName;

    @NotBlank(message = "Vui lòng nhập số điện thoại người nhận.")
    @Pattern(regexp = "^(?:\\+?84|0)[1-9][0-9]{8}$", message = "Số điện thoại không đúng định dạng Việt Nam.")
    private String phone;

    @NotNull(message = "Vui lòng chọn tỉnh/thành phố.")
    @Positive(message = "Tỉnh/thành phố không hợp lệ.")
    private Integer provinceId;

    @NotBlank(message = "Vui lòng chọn tỉnh/thành phố.")
    @Size(max = 100)
    private String province;

    @NotNull(message = "Vui lòng chọn quận/huyện.")
    @Positive(message = "Quận/huyện không hợp lệ.")
    private Integer districtId;

    @NotBlank(message = "Vui lòng chọn quận/huyện.")
    @Size(max = 100)
    private String district;

    @NotBlank(message = "Vui lòng chọn phường/xã.")
    @Size(max = 20)
    private String wardCode;

    @NotBlank(message = "Vui lòng chọn phường/xã.")
    @Size(max = 100)
    private String ward;

    @NotBlank(message = "Vui lòng nhập địa chỉ chi tiết.")
    @Size(max = 255, message = "Địa chỉ chi tiết tối đa 255 ký tự.")
    private String streetAddress;

    private boolean makeDefault;
}
