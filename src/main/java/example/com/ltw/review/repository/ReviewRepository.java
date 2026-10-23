package example.com.ltw.review.repository;

import example.com.ltw.review.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = "media")
    Optional<Review> findByOrderItemId(Long orderItemId);

    @EntityGraph(attributePaths = {"media", "user", "orderItem"})
    List<Review> findByOrderItemOrderIdAndUserId(Long orderId, Long userId);

    @EntityGraph(attributePaths = {"media", "user"})
    @Query("""
            select r from Review r
            where r.product.id = :productId
              and r.rating between 1 and 5
              and (r.comment is null or length(r.comment) <= 500)
            order by r.createdAt desc
            """)
    List<Review> findVisibleByProductId(@Param("productId") Long productId);

    @Query("""
            select r.product.id as productId, avg(r.rating) as averageRating, count(r.id) as reviewCount
            from Review r
            where r.product.id = :productId
              and r.rating between 1 and 5
              and (r.comment is null or length(r.comment) <= 500)
            group by r.product.id
            """)
    Optional<ReviewStatistics> findStatisticsByProductId(@Param("productId") Long productId);

    interface ReviewStatistics {
        Long getProductId();
        Double getAverageRating();
        Long getReviewCount();
    }
}
