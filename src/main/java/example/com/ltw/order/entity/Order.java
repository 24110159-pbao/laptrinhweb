package example.com.ltw.order.entity;

import example.com.ltw.branch.entity.Branch;
import example.com.ltw.discount.entity.DiscountCode;
import example.com.ltw.user.entity.User;
import example.com.ltw.checkout.entity.CheckoutSession;

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
        name = "orders",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_orders_order_code",
                        columnNames = "order_code"
                ),
                @UniqueConstraint(
                        name = "uk_orders_checkout_session",
                        columnNames = "checkout_session_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String orderCode;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_code_id")
    private DiscountCode discountCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkout_session_id", unique = true)
    private CheckoutSession checkoutSession;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false)
    private boolean discountAppliedToShipping;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal shippingFee;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String receiverName;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String receiverPhone;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String shippingProvince;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String shippingDistrict;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String shippingWard;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String shippingStreetAddress;

    @NotNull
    @Column(nullable = false)
    private Integer shippingProvinceId;

    @NotNull
    @Column(nullable = false)
    private Integer shippingDistrictId;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String shippingWardCode;

    @Size(max = 1000)
    @Column(length = 1000)
    private String note;

    @Column
    private LocalDateTime paymentExpiresAt;

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
            status = OrderStatus.PENDING;
        }

        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.UNPAID;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}