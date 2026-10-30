package example.com.ltw.address.controller;

import example.com.ltw.address.service.AddressLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequiredArgsConstructor
public class AddressLocationController {
    private final AddressLocationService addressLocationService;

    @GetMapping("/api/locations/provinces")
    public JsonNode provinces() {
        return addressLocationService.getProvinces();
    }

    @GetMapping("/api/locations/provinces/{provinceId}/districts")
    public JsonNode districts(@PathVariable Integer provinceId) {
        return addressLocationService.getDistricts(provinceId);
    }

    @GetMapping("/api/locations/districts/{districtId}/wards")
    public JsonNode wards(@PathVariable Integer districtId) {
        return addressLocationService.getWards(districtId);
    }
}
