package example.com.ltw.catalog.repository;

import example.com.ltw.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductId(Long productId);

    boolean existsByProductIdAndRamAndStorageAndColor(Long productId, String ram, String storage, String color);

    boolean existsByProductIdAndRamAndStorageAndColorAndIdNot(Long productId, String ram, String storage, String color, Long id);
}
