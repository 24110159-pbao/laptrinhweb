package example.com.ltw.address.service;

import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;

@Slf4j
@Service
public class AddressLocationService {
    private final RestClient restClient;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public AddressLocationService(
            @Value("${address.api.base-url:https://provinces.open-api.vn/api/v1}") String baseUrl,
            ObjectMapper objectMapper
    ) {
        this.restClient = RestClient.create();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.objectMapper = objectMapper;
    }

    public JsonNode getProvinces() {
        return get("/p/");
    }

    public JsonNode getDistricts(Integer provinceId) {
        return get("/p/{id}?depth=2", provinceId).path("districts");
    }

    public JsonNode getWards(Integer districtId) {
        return get("/d/{id}?depth=2", districtId).path("wards");
    }

    public void validate(AddressLocation location) {
        JsonNode province = get("/p/{id}?depth=2", location.provinceId());
        if (province.path("code").asInt(-1) != location.provinceId()
                || !province.path("name").asText().equals(location.province())) {
            throw new IllegalArgumentException("Tỉnh/thành phố đã chọn không hợp lệ.");
        }

        boolean districtBelongsToProvince = false;
        for (JsonNode district : province.path("districts")) {
            if (district.path("code").asInt(-1) == location.districtId()
                    && district.path("name").asText().equals(location.district())) {
                districtBelongsToProvince = true;
                break;
            }
        }
        if (!districtBelongsToProvince) {
            throw new IllegalArgumentException("Quận/huyện không thuộc tỉnh/thành phố đã chọn.");
        }

        boolean wardBelongsToDistrict = false;
        for (JsonNode ward : getWards(location.districtId())) {
            if (ward.path("code").asText().equals(location.wardCode())
                    && ward.path("name").asText().equals(location.ward())) {
                wardBelongsToDistrict = true;
                break;
            }
        }
        if (!wardBelongsToDistrict) {
            throw new IllegalArgumentException("Phường/xã không thuộc quận/huyện đã chọn.");
        }
    }

    private JsonNode get(String path, Object... uriVariables) {
        try {
            String requestUrl = baseUrl + path;
            for (Object uriVariable : uriVariables) {
                requestUrl = requestUrl.replaceFirst("\\{[^}]+}", uriVariable.toString());
            }
            ResponseEntity<String> response = restClient.get().uri(URI.create(requestUrl))
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Accept", "application/json")
                    .retrieve().toEntity(String.class);
            if (!response.hasBody() || response.getBody().isBlank()) {
                log.warn("Address API returned an empty body for {} (status {}, content type {})",
                        requestUrl, response.getStatusCode(), response.getHeaders().getContentType());
                throw new IllegalStateException("API địa giới hành chính trả về dữ liệu rỗng. Vui lòng thử lại.");
            }
            return objectMapper.readTree(response.getBody());
        } catch (RestClientException | JacksonException ex) {
            throw new IllegalStateException("Không thể tải dữ liệu địa giới hành chính. Vui lòng thử lại.", ex);
        }
    }

    public record AddressLocation(
            Integer provinceId,
            String province,
            Integer districtId,
            String district,
            String wardCode,
            String ward
    ) {
    }
}
