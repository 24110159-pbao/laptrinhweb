package example.com.ltw.inventory.repository;

import example.com.ltw.inventory.entity.BranchInventory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
