package example.com.ltw.inventory.repository;

import example.com.ltw.inventory.entity.BranchInventory;
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

public interface BranchInventoryRepository extends JpaRepository<BranchInventory, Long> {

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
