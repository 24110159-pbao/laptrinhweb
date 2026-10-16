package example.com.ltw.inventory.entity;

import example.com.ltw.branch.entity.Branch;
import example.com.ltw.catalog.entity.ProductVariant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.Check;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "branch_inventories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_branch_inventory_branch_variant",
                        columnNames = {"branch_id", "variant_id"}
                )
        }
)
@Check(
        constraints = """
        quantity >= 0
        AND reserved_quantity >= 0
        AND reserved_quantity <= quantity
        """
)
@Getter
@Setter
@NoArgsConstructor
public class BranchInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @PositiveOrZero
    @Column(nullable = false)
    private int quantity = 0;

    @PositiveOrZero
    @Column(nullable = false)
    private int reservedQuantity = 0;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (quantity < 0 || reservedQuantity < 0) {
            throw new IllegalStateException(
                    "Inventory quantities cannot be negative."
            );
        }

        if (reservedQuantity > quantity) {
            throw new IllegalStateException(
                    "Reserved quantity cannot exceed total quantity."
            );
        }

        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (quantity < 0 || reservedQuantity < 0) {
            throw new IllegalStateException(
                    "Inventory quantities cannot be negative."
            );
        }

        if (reservedQuantity > quantity) {
            throw new IllegalStateException(
                    "Reserved quantity cannot exceed total quantity."
            );
        }

        updatedAt = LocalDateTime.now();
    }

    public int getAvailableQuantity() {
        return quantity - reservedQuantity;
    }
}