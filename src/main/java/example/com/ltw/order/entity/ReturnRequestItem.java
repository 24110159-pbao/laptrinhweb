package example.com.ltw.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "return_request_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_return_request_order_item", columnNames = {"return_request_id", "order_item_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class ReturnRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Positive
    @Column(nullable = false)
    private int requestedQuantity;

    @Column(nullable = false)
    private int approvedQuantity;

    @Column(nullable = false)
    private int receivedQuantity;

    @Column(nullable = false)
    private int acceptedQuantity;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;
}
