package example.com.ltw.discount.repository;

import example.com.ltw.discount.entity.DiscountCode;
import example.com.ltw.discount.entity.DiscountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountCodeRepository extends JpaRepository<DiscountCode, Long> {

    Optional<DiscountCode> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<DiscountCode> findAllByOrderByStartAtDesc();

    List<DiscountCode> findAllByStatusOrderByStartAtDesc(DiscountStatus status);
}
