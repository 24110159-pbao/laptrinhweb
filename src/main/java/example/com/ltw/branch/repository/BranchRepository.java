package example.com.ltw.branch.repository;

import example.com.ltw.branch.entity.Branch;
import example.com.ltw.branch.entity.BranchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByStatusOrderByNameAsc(BranchStatus status);
}
