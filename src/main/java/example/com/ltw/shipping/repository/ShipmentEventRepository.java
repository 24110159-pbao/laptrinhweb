package example.com.ltw.shipping.repository;

import example.com.ltw.shipping.entity.ShipmentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, Long> {
    boolean existsByEventId(String eventId);
    List<ShipmentEvent> findAllByShipmentIdOrderByCreatedAtAsc(Long shipmentId);
    void deleteByShipmentId(Long shipmentId);
}
