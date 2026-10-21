package example.com.ltw.shipping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GhnCreateOrderResponse {

    private boolean success;
    private String orderCode;
    private String sortCode;
    private BigDecimal totalFee;
    private LocalDateTime expectedDeliveryTime;
    private String message;
    private boolean fallback;

    public String getTrackingUrl() {
        if (orderCode != null && !orderCode.isBlank()) {
            return "https://donhang.ghn.vn/?order_code=" + orderCode;
        }
        return null;
    }
}
