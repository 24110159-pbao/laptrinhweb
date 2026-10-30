package example.com.ltw.user.service;

import example.com.ltw.address.dto.AddressRequest;
import example.com.ltw.address.entity.Address;
import example.com.ltw.address.service.AddressLocationService;
import example.com.ltw.auth.service.AuthService;
import example.com.ltw.user.dto.ChangePasswordRequest;
import example.com.ltw.user.dto.ProfileUpdateRequest;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.repository.AddressRepository;
import example.com.ltw.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final AddressLocationService addressLocationService;
    private final CloudinaryAvatarService cloudinaryAvatarService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản đang đăng nhập."));
    }

    @Transactional
    public User updateProfile(Long userId, ProfileUpdateRequest request, HttpSession session) {
        User user = getUser(userId);
        user.setFullName(request.getFullName().trim());
        user.setPhone(normalizeOptional(request.getPhone()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());
        User savedUser = userRepository.save(user);
        session.setAttribute(AuthService.SESSION_CURRENT_USER, savedUser);
        return savedUser;
    }

    @Transactional
    public User updateAvatar(Long userId, MultipartFile file, HttpSession session) {
        User user = getUser(userId);
        user.setAvatarUrl(cloudinaryAvatarService.upload(file));
        User savedUser = userRepository.save(user);
        session.setAttribute(AuthService.SESSION_CURRENT_USER, savedUser);
        return savedUser;
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới xác nhận không khớp.");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<Address> getAddresses(Long userId) {
        return addressRepository.findByUserIdAndDeletedAtIsNullOrderByIsDefaultDescIdDesc(userId);
    }

    @Transactional(readOnly = true)
    public Address getAddress(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserIdAndDeletedAtIsNull(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ trong sổ địa chỉ của bạn."));
    }

    @Transactional
    public Address saveAddress(Long userId, AddressRequest request) {
        User user = getUser(userId);
        addressLocationService.validate(new AddressLocationService.AddressLocation(
                request.getProvinceId(), request.getProvince(),
                request.getDistrictId(), request.getDistrict(),
                request.getWardCode(), request.getWard()
        ));

        Address address;
        boolean isNew = request.getId() == null;
        if (isNew) {
            address = new Address();
            address.setUser(user);
        } else {
            address = getAddress(userId, request.getId());
        }

        boolean hasDefault = addressRepository
                .findFirstByUserIdAndDeletedAtIsNullAndIsDefaultTrue(userId).isPresent();
        boolean shouldBeDefault = request.isMakeDefault() || !hasDefault || (!isNew && address.isDefault());
        if (shouldBeDefault) {
            clearDefaultAddress(userId);
        }

        address.setReceiverName(request.getReceiverName().trim());
        address.setPhone(request.getPhone().trim());
        address.setProvinceId(request.getProvinceId());
        address.setProvince(request.getProvince().trim());
        address.setDistrictId(request.getDistrictId());
        address.setDistrict(request.getDistrict().trim());
        address.setWardCode(request.getWardCode().trim());
        address.setWard(request.getWard().trim());
        address.setStreetAddress(request.getStreetAddress().trim());
        address.setDefault(shouldBeDefault);
        return addressRepository.save(address);
    }

    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        Address selected = getAddress(userId, addressId);
        clearDefaultAddress(userId);
        selected.setDefault(true);
        addressRepository.save(selected);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = getAddress(userId, addressId);
        if (address.isDefault()) {
            throw new IllegalArgumentException("Hãy đặt một địa chỉ khác làm mặc định trước khi xóa địa chỉ này.");
        }
        address.setDeletedAt(LocalDateTime.now());
        addressRepository.save(address);
    }

    private void clearDefaultAddress(Long userId) {
        addressRepository.findByUserIdAndDeletedAtIsNullOrderByIsDefaultDescIdDesc(userId)
                .stream()
                .filter(Address::isDefault)
                .forEach(address -> address.setDefault(false));
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
