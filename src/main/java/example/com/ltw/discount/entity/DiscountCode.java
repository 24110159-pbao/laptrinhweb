package example.com.ltw.discount.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import example.com.ltw.catalog.entity.Category;
import example.com.ltw.catalog.entity.Product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "discount_codes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_discount_codes_code",
                        columnNames = "code"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String code;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal discountValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType type = DiscountType.PERCENTAGE;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal minimumOrderAmount = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal maximumDiscount;

    @Column(nullable = false)
    private boolean appliesToShipping;

    @Column
    private Integer perCustomerLimit;

    @ManyToMany
    @JoinTable(name = "discount_eligible_products",
            joinColumns = @JoinColumn(name = "discount_code_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id"))
    private Set<Product> eligibleProducts = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "discount_eligible_categories",
            joinColumns = @JoinColumn(name = "discount_code_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> eligibleCategories = new HashSet<>();

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer quantity;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime startAt;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountStatus status = DiscountStatus.ACTIVE;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = DiscountStatus.ACTIVE;
        }
        if (type == null) type = DiscountType.PERCENTAGE;
        if (minimumOrderAmount == null) minimumOrderAmount = BigDecimal.ZERO;
    }

    @PostLoad
    protected void applyLegacyDefaults() {
        if (type == null) type = DiscountType.PERCENTAGE;
        if (minimumOrderAmount == null) minimumOrderAmount = BigDecimal.ZERO;
    }
}