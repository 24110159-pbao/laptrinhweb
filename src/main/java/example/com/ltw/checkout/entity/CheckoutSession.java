package example.com.ltw.checkout.entity;

import example.com.ltw.cart.entity.Cart;
import example.com.ltw.discount.entity.DiscountCode;
import example.com.ltw.user.entity.User;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "checkout_sessions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_checkout_sessions_order_code", columnNames = "order_code")
})
@Getter
@Setter
@NoArgsConstructor
public class CheckoutSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String orderCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CheckoutStatus status = CheckoutStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean discountAppliedToShipping;

    @Column(nullable = false)
    private boolean discountReserved;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal finalAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_code_id")
    private DiscountCode discountCode;

    @Column(length = 50)
    private String discountCodeValue;

    @Column(length = 150)
    private String receiverName;

    @Column(length = 20)
    private String receiverPhone;

    @Column(length = 100)
    private String shippingProvince;

    @Column(length = 100)
    private String shippingDistrict;

    @Column(length = 100)
    private String shippingWard;

    @Column(length = 255)
    private String shippingStreetAddress;

    private Integer shippingProvinceId;
    private Integer shippingDistrictId;

    @Column(length = 20)
    private String shippingWardCode;

    private Long shippingAddressId;

    @Column(length = 100)
    private String ghnQuoteToken;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (startedAt == null) startedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
