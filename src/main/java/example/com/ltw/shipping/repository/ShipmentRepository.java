package example.com.ltw.shipping.repository;

import example.com.ltw.shipping.entity.Shipment;
import example.com.ltw.shipping.entity.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByOrder_Id(Long orderId);

    List<Shipment> findAllByShipper_IdOrderByCreatedAtDesc(Long shipperId);

    Page<Shipment> findAllByShipper_IdOrderByCreatedAtDesc(Long shipperId, Pageable pageable);

    Page<Shipment> findAllByShipper_IdAndStatusOrderByCreatedAtDesc(Long shipperId, ShipmentStatus status, Pageable pageable);

    Optional<Shipment> findByIdAndShipper_Id(Long id, Long shipperId);

    Optional<Shipment> findByGhnOrderCode(String ghnOrderCode);

    long countByShipper_IdAndStatus(Long shipperId, ShipmentStatus status);

    long countByShipper_Id(Long shipperId);

    List<Shipment> findTop5ByShipper_IdOrderByCreatedAtDesc(Long shipperId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM Shipment s WHERE s.shipper.id = :shipperId " +
            "AND (:fromDate IS NULL OR s.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR s.createdAt <= :toDate)")
    long countByShipperAndTime(
            @org.springframework.data.repository.query.Param("shipperId") Long shipperId,
            @org.springframework.data.repository.query.Param("fromDate") java.time.LocalDateTime fromDate,
            @org.springframework.data.repository.query.Param("toDate") java.time.LocalDateTime toDate
    );

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM Shipment s WHERE s.shipper.id = :shipperId " +
            "AND s.status = :status " +
            "AND s.order.status != example.com.ltw.order.entity.OrderStatus.CANCELLED " +
            "AND (:fromDate IS NULL OR s.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR s.createdAt <= :toDate)")
    long countActiveByShipperAndStatusAndTime(
            @org.springframework.data.repository.query.Param("shipperId") Long shipperId,
            @org.springframework.data.repository.query.Param("status") ShipmentStatus status,
            @org.springframework.data.repository.query.Param("fromDate") java.time.LocalDateTime fromDate,
            @org.springframework.data.repository.query.Param("toDate") java.time.LocalDateTime toDate
    );

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM Shipment s WHERE s.shipper.id = :shipperId " +
            "AND s.status = example.com.ltw.shipping.entity.ShipmentStatus.DELIVERED " +
            "AND (:fromDate IS NULL OR s.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR s.createdAt <= :toDate)")
    long countDeliveredByShipperAndTime(
            @org.springframework.data.repository.query.Param("shipperId") Long shipperId,
            @org.springframework.data.repository.query.Param("fromDate") java.time.LocalDateTime fromDate,
            @org.springframework.data.repository.query.Param("toDate") java.time.LocalDateTime toDate
    );

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM Shipment s WHERE s.shipper.id = :shipperId " +
            "AND (s.status IN (example.com.ltw.shipping.entity.ShipmentStatus.DELIVERY_FAILED, example.com.ltw.shipping.entity.ShipmentStatus.CANCELLED) OR s.order.status = example.com.ltw.order.entity.OrderStatus.CANCELLED) " +
            "AND s.status != example.com.ltw.shipping.entity.ShipmentStatus.DELIVERED " +
            "AND (:fromDate IS NULL OR s.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR s.createdAt <= :toDate)")
    long countFailedByShipperAndTime(
            @org.springframework.data.repository.query.Param("shipperId") Long shipperId,
            @org.springframework.data.repository.query.Param("fromDate") java.time.LocalDateTime fromDate,
            @org.springframework.data.repository.query.Param("toDate") java.time.LocalDateTime toDate
    );

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(s.order.finalAmount), 0) FROM Shipment s WHERE s.shipper.id = :shipperId " +
            "AND s.status IN (example.com.ltw.shipping.entity.ShipmentStatus.PENDING, example.com.ltw.shipping.entity.ShipmentStatus.RECEIVED) " +
            "AND s.order.paymentStatus = example.com.ltw.order.entity.PaymentStatus.UNPAID " +
            "AND s.order.status != example.com.ltw.order.entity.OrderStatus.CANCELLED " +
            "AND (:fromDate IS NULL OR s.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR s.createdAt <= :toDate)")
    java.math.BigDecimal sumCodToCollectByShipperAndTime(
            @org.springframework.data.repository.query.Param("shipperId") Long shipperId,
            @org.springframework.data.repository.query.Param("fromDate") java.time.LocalDateTime fromDate,
            @org.springframework.data.repository.query.Param("toDate") java.time.LocalDateTime toDate
    );

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(s.order.finalAmount), 0) FROM Shipment s WHERE s.shipper.id = :shipperId " +
            "AND s.status = example.com.ltw.shipping.entity.ShipmentStatus.DELIVERED " +
            "AND s.order.paymentMethod = example.com.ltw.order.entity.PaymentMethod.COD " +
            "AND (:fromDate IS NULL OR s.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR s.createdAt <= :toDate)")
    java.math.BigDecimal sumCodCollectedByShipperAndTime(
            @org.springframework.data.repository.query.Param("shipperId") Long shipperId,
            @org.springframework.data.repository.query.Param("fromDate") java.time.LocalDateTime fromDate,
            @org.springframework.data.repository.query.Param("toDate") java.time.LocalDateTime toDate
    );

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM Shipment s WHERE s.shipper.id = :shipperId " +
            "AND s.order.paymentStatus = example.com.ltw.order.entity.PaymentStatus.PAID " +
            "AND (:fromDate IS NULL OR s.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR s.createdAt <= :toDate)")
    long countPaidOnlineByShipperAndTime(
            @org.springframework.data.repository.query.Param("shipperId") Long shipperId,
            @org.springframework.data.repository.query.Param("fromDate") java.time.LocalDateTime fromDate,
            @org.springframework.data.repository.query.Param("toDate") java.time.LocalDateTime toDate
    );

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Shipment s JOIN FETCH s.order WHERE s.shipper.id = :shipperId " +
            "AND (:fromDate IS NULL OR s.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR s.createdAt <= :toDate) " +
            "ORDER BY s.createdAt DESC")
    List<Shipment> findRecentShipmentsByShipperAndTime(
            @org.springframework.data.repository.query.Param("shipperId") Long shipperId,
            @org.springframework.data.repository.query.Param("fromDate") java.time.LocalDateTime fromDate,
            @org.springframework.data.repository.query.Param("toDate") java.time.LocalDateTime toDate,
            Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Shipment s JOIN FETCH s.order WHERE s.shipper.id = :shipperId " +
            "AND s.status = example.com.ltw.shipping.entity.ShipmentStatus.PENDING " +
            "AND s.order.status != example.com.ltw.order.entity.OrderStatus.CANCELLED " +
            "AND (:fromDate IS NULL OR s.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR s.createdAt <= :toDate) " +
            "ORDER BY s.createdAt DESC")
    List<Shipment> findUrgentShipmentsByShipperAndTime(
            @org.springframework.data.repository.query.Param("shipperId") Long shipperId,
            @org.springframework.data.repository.query.Param("fromDate") java.time.LocalDateTime fromDate,
            @org.springframework.data.repository.query.Param("toDate") java.time.LocalDateTime toDate,
            Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT s FROM Shipment s JOIN FETCH s.order WHERE s.shipper.id = :shipperId " +
                    "AND (:fromDate IS NULL OR s.createdAt >= :fromDate) " +
                    "AND (:toDate IS NULL OR s.createdAt <= :toDate) " +
                    "AND (:keyword IS NULL OR :keyword = '' OR " +
                    "     LOWER(s.ghnOrderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.receiverName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.receiverPhone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                    "ORDER BY s.createdAt DESC",
            countQuery = "SELECT COUNT(s) FROM Shipment s WHERE s.shipper.id = :shipperId " +
                    "AND (:fromDate IS NULL OR s.createdAt >= :fromDate) " +
                    "AND (:toDate IS NULL OR s.createdAt <= :toDate) " +
                    "AND (:keyword IS NULL OR :keyword = '' OR " +
                    "     LOWER(s.ghnOrderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.receiverName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.receiverPhone) LIKE LOWER(CONCAT('%', :keyword, '%')))"
    )
    Page<Shipment> searchAllShipperTasks(
            @org.springframework.data.repository.query.Param("shipperId") Long shipperId,
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            @org.springframework.data.repository.query.Param("fromDate") java.time.LocalDateTime fromDate,
            @org.springframework.data.repository.query.Param("toDate") java.time.LocalDateTime toDate,
            Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT s FROM Shipment s JOIN FETCH s.order WHERE s.shipper.id = :shipperId " +
                    "AND s.status = :status " +
                    "AND s.order.status != example.com.ltw.order.entity.OrderStatus.CANCELLED " +
                    "AND (:fromDate IS NULL OR s.createdAt >= :fromDate) " +
                    "AND (:toDate IS NULL OR s.createdAt <= :toDate) " +
                    "AND (:keyword IS NULL OR :keyword = '' OR " +
                    "     LOWER(s.ghnOrderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.receiverName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.receiverPhone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                    "ORDER BY s.createdAt DESC",
            countQuery = "SELECT COUNT(s) FROM Shipment s WHERE s.shipper.id = :shipperId " +
                    "AND s.status = :status " +
                    "AND s.order.status != example.com.ltw.order.entity.OrderStatus.CANCELLED " +
                    "AND (:fromDate IS NULL OR s.createdAt >= :fromDate) " +
                    "AND (:toDate IS NULL OR s.createdAt <= :toDate) " +
                    "AND (:keyword IS NULL OR :keyword = '' OR " +
                    "     LOWER(s.ghnOrderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.receiverName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.receiverPhone) LIKE LOWER(CONCAT('%', :keyword, '%')))"
    )
    Page<Shipment> searchActiveShipperTasksByStatus(
            @org.springframework.data.repository.query.Param("shipperId") Long shipperId,
            @org.springframework.data.repository.query.Param("status") ShipmentStatus status,
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            @org.springframework.data.repository.query.Param("fromDate") java.time.LocalDateTime fromDate,
            @org.springframework.data.repository.query.Param("toDate") java.time.LocalDateTime toDate,
            Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT s FROM Shipment s JOIN FETCH s.order WHERE s.shipper.id = :shipperId " +
                    "AND s.status = example.com.ltw.shipping.entity.ShipmentStatus.DELIVERED " +
                    "AND (:fromDate IS NULL OR s.createdAt >= :fromDate) " +
                    "AND (:toDate IS NULL OR s.createdAt <= :toDate) " +
                    "AND (:keyword IS NULL OR :keyword = '' OR " +
                    "     LOWER(s.ghnOrderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.receiverName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.receiverPhone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                    "ORDER BY s.createdAt DESC",
            countQuery = "SELECT COUNT(s) FROM Shipment s WHERE s.shipper.id = :shipperId " +
                    "AND s.status = example.com.ltw.shipping.entity.ShipmentStatus.DELIVERED " +
                    "AND (:fromDate IS NULL OR s.createdAt >= :fromDate) " +
                    "AND (:toDate IS NULL OR s.createdAt <= :toDate) " +
                    "AND (:keyword IS NULL OR :keyword = '' OR " +
                    "     LOWER(s.ghnOrderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.receiverName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.receiverPhone) LIKE LOWER(CONCAT('%', :keyword, '%')))"
    )
    Page<Shipment> searchDeliveredShipperTasks(
            @org.springframework.data.repository.query.Param("shipperId") Long shipperId,
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            @org.springframework.data.repository.query.Param("fromDate") java.time.LocalDateTime fromDate,
            @org.springframework.data.repository.query.Param("toDate") java.time.LocalDateTime toDate,
            Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT s FROM Shipment s JOIN FETCH s.order WHERE s.shipper.id = :shipperId " +
                    "AND (s.status IN (example.com.ltw.shipping.entity.ShipmentStatus.DELIVERY_FAILED, example.com.ltw.shipping.entity.ShipmentStatus.CANCELLED) OR s.order.status = example.com.ltw.order.entity.OrderStatus.CANCELLED) " +
                    "AND s.status != example.com.ltw.shipping.entity.ShipmentStatus.DELIVERED " +
                    "AND (:fromDate IS NULL OR s.createdAt >= :fromDate) " +
                    "AND (:toDate IS NULL OR s.createdAt <= :toDate) " +
                    "AND (:keyword IS NULL OR :keyword = '' OR " +
                    "     LOWER(s.ghnOrderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.receiverName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.receiverPhone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                    "ORDER BY s.createdAt DESC",
            countQuery = "SELECT COUNT(s) FROM Shipment s WHERE s.shipper.id = :shipperId " +
                    "AND (s.status IN (example.com.ltw.shipping.entity.ShipmentStatus.DELIVERY_FAILED, example.com.ltw.shipping.entity.ShipmentStatus.CANCELLED) OR s.order.status = example.com.ltw.order.entity.OrderStatus.CANCELLED) " +
                    "AND s.status != example.com.ltw.shipping.entity.ShipmentStatus.DELIVERED " +
                    "AND (:fromDate IS NULL OR s.createdAt >= :fromDate) " +
                    "AND (:toDate IS NULL OR s.createdAt <= :toDate) " +
                    "AND (:keyword IS NULL OR :keyword = '' OR " +
                    "     LOWER(s.ghnOrderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.receiverName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "     LOWER(s.order.receiverPhone) LIKE LOWER(CONCAT('%', :keyword, '%')))"
    )
    Page<Shipment> searchFailedShipperTasks(
            @org.springframework.data.repository.query.Param("shipperId") Long shipperId,
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            @org.springframework.data.repository.query.Param("fromDate") java.time.LocalDateTime fromDate,
            @org.springframework.data.repository.query.Param("toDate") java.time.LocalDateTime toDate,
            Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM Shipment s WHERE s.shipper.id = :shipperId " +
            "AND s.status IN (example.com.ltw.shipping.entity.ShipmentStatus.PENDING, example.com.ltw.shipping.entity.ShipmentStatus.RECEIVED) " +
            "AND s.order.status != example.com.ltw.order.entity.OrderStatus.CANCELLED")
    long countActiveDeliveriesByShipper(@org.springframework.data.repository.query.Param("shipperId") Long shipperId);

    @org.springframework.data.jpa.repository.Query("SELECT MAX(s.createdAt) FROM Shipment s WHERE s.shipper.id = :shipperId")
    java.time.LocalDateTime findLatestAssignmentTimeByShipper(@org.springframework.data.repository.query.Param("shipperId") Long shipperId);
}
