package example.com.ltw.inventory.repository;

import example.com.ltw.inventory.entity.BranchInventory;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import example.com.ltw.branch.entity.BranchStatus;
import example.com.ltw.catalog.entity.ProductStatus;
import example.com.ltw.catalog.entity.ProductVariantStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchInventoryRepository extends JpaRepository<BranchInventory, Long> {

    @EntityGraph(attributePaths = {"variant", "variant.product"})
    List<BranchInventory> findAllByBranch_IdOrderByIdAsc(Long branchId);

    Optional<BranchInventory> findByIdAndBranch_Id(Long id, Long branchId);

    long countByBranch_Id(Long branchId);

    @EntityGraph(attributePaths = {"variant", "variant.product"})
    Optional<BranchInventory> findByBranch_IdAndVariant_Id(Long branchId, Long variantId);

    @EntityGraph(attributePaths = {"variant", "variant.product"})
    List<BranchInventory> findAllByBranch_IdAndVariant_Product_Id(Long branchId, Long productId);

    @Query("""
            select i from BranchInventory i
            join fetch i.branch b
            join fetch i.variant v
            join fetch v.product p
            where b.status = :branchStatus
              and v.status = :variantStatus
              and p.status = :productStatus
            """)
    List<BranchInventory> findCatalogInventory(
            @Param("branchStatus") BranchStatus branchStatus,
            @Param("variantStatus") ProductVariantStatus variantStatus,
            @Param("productStatus") ProductStatus productStatus
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from BranchInventory i where i.id = :id")
    Optional<BranchInventory> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from BranchInventory i where i.branch.id = :branchId and i.variant.id = :variantId")
    Optional<BranchInventory> findByBranchIdAndVariantIdForUpdate(
            @Param("branchId") Long branchId, @Param("variantId") Long variantId
    );


}
