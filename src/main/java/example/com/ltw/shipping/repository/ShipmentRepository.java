package example.com.ltw.shipping.repository;

import example.com.ltw.shipping.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByOrderId(Long orderId);

    boolean existsByOrderIdAndStatusAndDeliveredAtIsNotNull(
            Long orderId,
            example.com.ltw.shipping.entity.ShipmentStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shipment s join fetch s.order where s.ghnOrderCode = :code")
    Optional<Shipment> findByGhnOrderCodeForUpdate(@Param("code") String code);

    Optional<Shipment> findByGhnOrderCode(String code);

    boolean existsByGhnOrderCode(String code);

    void deleteByOrderId(Long orderId);
}
