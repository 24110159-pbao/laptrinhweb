package example.com.ltw.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "product_variants",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_variant_configuration",
                        columnNames = {
                                "product_id",
                                "ram",
                                "storage",
                                "color"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    private String ram;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    private String storage;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String color;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @DecimalMin("0.0")
    @Column(precision = 15, scale = 2)
    private BigDecimal salePrice;

    @Column
    private Integer shippingWeightGrams;

    @Column
    private Integer shippingLengthCm;

    @Column
    private Integer shippingWidthCm;

    @Column
    private Integer shippingHeightCm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductVariantStatus status =
            ProductVariantStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;

        if (status == null) {
            status = ProductVariantStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}