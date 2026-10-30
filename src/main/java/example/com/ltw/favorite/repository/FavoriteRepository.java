package example.com.ltw.favorite.repository;

import example.com.ltw.catalog.entity.ProductStatus;
import example.com.ltw.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    Optional<Favorite> findByUserIdAndProductId(Long userId, Long productId);

    @Query("""
            select f from Favorite f
            join fetch f.product
            where f.user.id = :userId and f.product.status = :status
            order by f.createdAt desc
            """)
    List<Favorite> findVisibleByUserId(
            @Param("userId") Long userId,
            @Param("status") ProductStatus status
    );
}
