package example.com.ltw.checkout.entity;

import example.com.ltw.inventory.entity.BranchInventory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_reservations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_reservation_checkout_inventory", columnNames = {"checkout_session_id", "inventory_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checkout_session_id", nullable = false)
    private CheckoutSession checkoutSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false)
    private BranchInventory inventory;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean convertedToOrder;

    @Column
    private LocalDateTime convertedAt;

    @Column
    private LocalDateTime releasedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (quantity <= 0) throw new IllegalStateException("Reservation quantity must be positive.");
        createdAt = LocalDateTime.now();
    }
}
