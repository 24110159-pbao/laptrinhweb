package example.com.ltw.checkout.repository;

import example.com.ltw.checkout.entity.CheckoutSession;
import example.com.ltw.checkout.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
    List<InventoryReservation> findByCheckoutSessionAndConvertedToOrderFalse(CheckoutSession checkoutSession);

    Optional<InventoryReservation> findByCheckoutSessionIdAndInventoryIdAndConvertedToOrderFalse(
            Long checkoutSessionId, Long inventoryId
    );
}
