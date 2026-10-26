package example.com.ltw.discount.repository;

import example.com.ltw.discount.entity.DiscountCode;

import example.com.ltw.discount.entity.DiscountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountCodeRepository extends JpaRepository<DiscountCode, Long> {


    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<DiscountCode> findAllByOrderByStartAtDesc();

    List<DiscountCode> findAllByStatusOrderByStartAtDesc(DiscountStatus status);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct d from DiscountCode d left join fetch d.eligibleProducts p left join fetch p.category left join fetch d.eligibleCategories where upper(d.code) = upper(:code)")
    Optional<DiscountCode> findByCodeForUpdate(@Param("code") String code);
}
