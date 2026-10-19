package example.com.ltw.shipping.service;

import example.com.ltw.branch.entity.Branch;
import example.com.ltw.cart.entity.CartItem;
import example.com.ltw.catalog.entity.ProductVariant;
import example.com.ltw.checkout.entity.CheckoutSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GhnShippingService {

    private final RestClient restClient;
    private final String baseUrl;
    private final String token;
    private final int shopId;
    private final int serviceTypeId;

    public GhnShippingService(
            @Value("${ghn.api.base-url:https://dev-online-gateway.ghn.vn/shiip/public-api}") String baseUrl,
            @Value("${ghn.api.token:}") String token,
            @Value("${ghn.shop-id:0}") int shopId,
            @Value("${ghn.service-type-id:0}") int serviceTypeId
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.token = token;
        this.shopId = shopId;
        this.serviceTypeId = serviceTypeId;
    }

    public BigDecimal quote(Branch branch, List<CartItem> items, CheckoutSession checkout) {
        validateConfiguration(branch, items, checkout);
        int weight = 0;
        int length = 1;
        int width = 1;
        int height = 0;
        try {
            for (CartItem item : items) {
                ProductVariant variant = item.getInventory().getVariant();
                weight = Math.addExact(weight, Math.multiplyExact(variant.getShippingWeightGrams(), item.getQuantity()));
                length = Math.max(length, variant.getShippingLengthCm());
                width = Math.max(width, variant.getShippingWidthCm());
                height = Math.addExact(height, Math.multiplyExact(variant.getShippingHeightCm(), item.getQuantity()));
            }
        } catch (ArithmeticException ex) {
            throw new IllegalStateException("Quy cách kiện hàng vượt giới hạn GHN.", ex);
        }

        Map<String, Object> request = new HashMap<>();
        request.put("from_district_id", branch.getGhnDistrictId());
        request.put("from_ward_code", branch.getGhnWardCode());
        request.put("to_district_id", checkout.getShippingDistrictId());
        request.put("to_ward_code", checkout.getShippingWardCode());
        request.put("service_type_id", serviceTypeId);
        request.put("weight", weight);
        request.put("length", length);
        request.put("width", width);
        request.put("height", height);
        try {
            request.put("insurance_value", checkout.getSubtotal().intValueExact());
        } catch (ArithmeticException ex) {
            throw new IllegalStateException("Giá trị kiện hàng vượt giới hạn dữ liệu GHN.", ex);
        }

        try {
            Map<String, Object> response = restClient.post()
                    .uri(baseUrl + "/v2/shipping-order/fee")
                    .header("Token", token)
                    .header("ShopId", Integer.toString(shopId))
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null || !(response.get("code") instanceof Number code) || code.intValue() != 200
                    || !(response.get("data") instanceof Map<?, ?> data)
                    || !(data.get("total") instanceof Number total)) {
                throw new IllegalStateException("GHN không trả về phí vận chuyển hợp lệ.");
            }
            BigDecimal fee = new BigDecimal(total.toString());
            if (fee.signum() < 0) throw new IllegalStateException("GHN trả về phí vận chuyển không hợp lệ.");
            return BigDecimal.valueOf(fee.longValueExact());
        } catch (ArithmeticException ex) {
            throw new IllegalStateException("GHN trả về phí vận chuyển không hợp lệ.", ex);
        } catch (RestClientException ex) {
            log.error("GHN fee request failed for checkout {}", checkout.getId(), ex);
            throw new IllegalStateException("Không thể kết nối GHN để tính phí vận chuyển. Vui lòng thử lại.", ex);
        }
    }

    private void validateConfiguration(Branch branch, List<CartItem> items, CheckoutSession checkout) {
        if (token.isBlank() || shopId <= 0 || serviceTypeId <= 0
                || branch.getGhnDistrictId() == null || branch.getGhnDistrictId() <= 0
                || branch.getGhnWardCode() == null || branch.getGhnWardCode().isBlank()) {
            throw new IllegalStateException("Thiếu cấu hình GHN cho hệ thống hoặc chi nhánh gửi hàng.");
        }
        if (checkout.getShippingDistrictId() == null || checkout.getShippingWardCode() == null
                || checkout.getShippingWardCode().isBlank()) {
            throw new IllegalArgumentException("Địa chỉ nhận hàng chưa đủ mã hành chính GHN.");
        }
        for (CartItem item : items) {
            ProductVariant variant = item.getInventory().getVariant();
            if (variant.getShippingWeightGrams() == null || variant.getShippingWeightGrams() <= 0
                    || variant.getShippingLengthCm() == null || variant.getShippingLengthCm() <= 0
                    || variant.getShippingWidthCm() == null || variant.getShippingWidthCm() <= 0
                    || variant.getShippingHeightCm() == null || variant.getShippingHeightCm() <= 0) {
                throw new IllegalStateException("Thiếu cấu hình quy cách đóng gói cho một hoặc nhiều sản phẩm.");
            }
        }
    }
}
