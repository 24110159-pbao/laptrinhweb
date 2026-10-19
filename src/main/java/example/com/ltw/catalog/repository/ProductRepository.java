package example.com.ltw.catalog.repository;

import example.com.ltw.catalog.entity.Product;
import example.com.ltw.catalog.entity.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStatus(ProductStatus status);

    @Query("SELECT p FROM Product p WHERE p.status = :status AND LOWER(p.brand.name) = LOWER(:brandName)")
    List<Product> findByStatusAndBrandName(@Param("status") ProductStatus status, @Param("brandName") String brandName);

    @Query("SELECT p FROM Product p WHERE p.status = :status AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.brand.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchProducts(@Param("status") ProductStatus status, @Param("keyword") String keyword);

    Optional<Product> findBySlug(String slug);

    Optional<Product> findBySlugAndStatus(String slug, ProductStatus status);

    Optional<Product> findByIdAndStatus(Long id, ProductStatus status);
}
