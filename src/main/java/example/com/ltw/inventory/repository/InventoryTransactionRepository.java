package example.com.ltw.inventory.repository;

import example.com.ltw.inventory.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
}
