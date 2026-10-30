package example.com.ltw.inventory.entity;

import example.com.ltw.branch.entity.Branch;
import example.com.ltw.catalog.entity.ProductVariant;
import example.com.ltw.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventory_transactions",
        indexes = {
                @Index(
                        name = "idx_inventory_tx_variant_branch",
                        columnList = "variant_id, branch_id"
                ),
                @Index(
                        name = "idx_inventory_tx_created_at",
                        columnList = "created_at"
                ),
                @Index(
                        name = "idx_inventory_tx_type",
                        columnList = "type"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "variant_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_inventory_tx_variant")
    )
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "branch_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_inventory_tx_branch")
    )
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InventoryTransactionType type;

    @Column(nullable = false)
    private int quantityChange;

    @Column(nullable = false)
    private int quantityBefore;

    @Column(nullable = false)
    private int quantityAfter;

    @Column(length = 100)
    private String referenceCode;

    @Column(length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            foreignKey = @ForeignKey(name = "fk_inventory_tx_user")
    )
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (quantityBefore < 0 || quantityAfter < 0) {
            throw new IllegalStateException(
                    "Inventory transaction quantities cannot be negative"
            );
        }

        if (quantityChange == 0) {
            throw new IllegalStateException(
                    "Inventory transaction quantity change cannot be zero"
            );
        }

        if (quantityAfter - quantityBefore != quantityChange) {
            throw new IllegalStateException(
                    "Inventory transaction quantities are inconsistent"
            );
        }

        createdAt = LocalDateTime.now();
    }

    public static InventoryTransaction create(
            ProductVariant variant, Branch branch, InventoryTransactionType type,
            int quantityBefore, int quantityAfter, String referenceCode, String note, User createdBy
    ) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setVariant(variant);
        transaction.setBranch(branch);
        transaction.setType(type);
        transaction.setQuantityBefore(quantityBefore);
        transaction.setQuantityAfter(quantityAfter);
        transaction.setQuantityChange(quantityAfter - quantityBefore);
        transaction.setReferenceCode(referenceCode);
        transaction.setNote(note);
        transaction.setCreatedBy(createdBy);
        return transaction;
    }
}