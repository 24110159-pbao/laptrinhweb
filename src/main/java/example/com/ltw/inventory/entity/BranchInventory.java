package example.com.ltw.inventory.entity;

import example.com.ltw.branch.entity.Branch;
import example.com.ltw.catalog.entity.ProductVariantStatus;
import example.com.ltw.catalog.entity.ProductVariant;

import jakarta.persistence.*;

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

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductVariantStatus status = ProductVariantStatus.ACTIVE;

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

        if (status == null) {
            status = ProductVariantStatus.ACTIVE;
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

        if (status == null) {
            status = ProductVariantStatus.ACTIVE;
        }

        updatedAt = LocalDateTime.now();
    }

    public int getAvailableQuantity() {
        return quantity - reservedQuantity;
    }
}