package example.com.ltw.user.repository;

import example.com.ltw.address.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserIdAndDeletedAtIsNullOrderByIsDefaultDescIdDesc(Long userId);

    Optional<Address> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    Optional<Address> findFirstByUserIdAndDeletedAtIsNullAndIsDefaultTrue(Long userId);
}
