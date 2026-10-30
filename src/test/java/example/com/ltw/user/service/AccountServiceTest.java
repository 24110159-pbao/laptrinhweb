package example.com.ltw.user.service;

import example.com.ltw.address.dto.AddressRequest;
import example.com.ltw.address.entity.Address;
import example.com.ltw.address.service.AddressLocationService;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.repository.AddressRepository;
import example.com.ltw.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private AddressLocationService addressLocationService;
    @Mock
    private CloudinaryAvatarService cloudinaryAvatarService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private AccountService accountService;

    @Test
    void firstAddressBecomesDefault() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findFirstByUserIdAndDeletedAtIsNullAndIsDefaultTrue(1L)).thenReturn(Optional.empty());
        when(addressRepository.findByUserIdAndDeletedAtIsNullOrderByIsDefaultDescIdDesc(1L)).thenReturn(List.of());
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Address saved = accountService.saveAddress(1L, validAddressRequest());

        assertTrue(saved.isDefault());
        assertTrue(saved.getUser() == user);
        verify(addressLocationService).validate(any(AddressLocationService.AddressLocation.class));
    }

    @Test
    void laterAddressDoesNotReplaceExistingDefault() {
        User user = new User();
        user.setId(1L);
        Address existingDefault = new Address();
        existingDefault.setDefault(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findFirstByUserIdAndDeletedAtIsNullAndIsDefaultTrue(1L))
                .thenReturn(Optional.of(existingDefault));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Address saved = accountService.saveAddress(1L, validAddressRequest());

        assertFalse(saved.isDefault());
        assertTrue(existingDefault.isDefault());
    }

    @Test
    void defaultAddressCannotBeDeleted() {
        Address address = new Address();
        address.setDefault(true);
        when(addressRepository.findByIdAndUserIdAndDeletedAtIsNull(5L, 1L)).thenReturn(Optional.of(address));

        assertThrows(IllegalArgumentException.class, () -> accountService.deleteAddress(1L, 5L));

        verify(addressRepository, never()).save(any(Address.class));
    }

    private AddressRequest validAddressRequest() {
        AddressRequest request = new AddressRequest();
        request.setReceiverName("Nguyen Van A");
        request.setPhone("0912345678");
        request.setProvinceId(1);
        request.setProvince("Ha Noi");
        request.setDistrictId(1);
        request.setDistrict("Ba Dinh");
        request.setWardCode("1");
        request.setWard("Phuc Xa");
        request.setStreetAddress("1 Example Street");
        return request;
    }
}
