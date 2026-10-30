package example.com.ltw.checkout.service;

import example.com.ltw.address.dto.AddressRequest;
import example.com.ltw.address.entity.Address;
import example.com.ltw.address.service.AddressLocationService;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.repository.AddressRepository;
import example.com.ltw.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckoutAddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressLocationService addressLocationService;

    @Transactional(readOnly = true)
    public Address getExistingAddress(Long addressId, Long userId) {
        return addressRepository.findByIdAndUserIdAndDeletedAtIsNull(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ nhận hàng không thuộc tài khoản của bạn."));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Address saveNewAddress(Long userId, AddressRequest request) {
        validate(request);
        addressLocationService.validate(new AddressLocationService.AddressLocation(
                request.getProvinceId(), request.getProvince(),
                request.getDistrictId(), request.getDistrict(),
                request.getWardCode(), request.getWard()
        ));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản khách hàng."));
        Address address = new Address();
        address.setUser(user);
        address.setReceiverName(request.getReceiverName().trim());
        address.setPhone(request.getPhone().trim());
        address.setProvinceId(request.getProvinceId());
        address.setProvince(request.getProvince().trim());
        address.setDistrictId(request.getDistrictId());
        address.setDistrict(request.getDistrict().trim());
        address.setWardCode(request.getWardCode().trim());
        address.setWard(request.getWard().trim());
        address.setStreetAddress(request.getStreetAddress().trim());
        address.setDefault(false);
        return addressRepository.save(address);
    }

    private void validate(AddressRequest address) {
        if (address == null || address.getReceiverName() == null || address.getReceiverName().isBlank()
                || address.getReceiverName().length() > 150
                || address.getPhone() == null || !address.getPhone().matches("^(?:\\+?84|0)[1-9][0-9]{8}$")
                || address.getProvinceId() == null || address.getProvince() == null
                || address.getProvince().length() > 100
                || address.getDistrictId() == null || address.getDistrict() == null
                || address.getDistrict().length() > 100
                || address.getWardCode() == null || address.getWard() == null
                || address.getWardCode().length() > 20 || address.getWard().length() > 100
                || address.getStreetAddress() == null || address.getStreetAddress().isBlank()
                || address.getStreetAddress().length() > 255) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ thông tin người nhận và địa chỉ hợp lệ.");
        }
    }
}
