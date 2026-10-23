package example.com.ltw.branch.repository;

import example.com.ltw.branch.entity.Branch;
import example.com.ltw.branch.entity.BranchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    List<Branch> findByStatus(BranchStatus status);

    Optional<Branch> findFirstByStatusOrderByIdAsc(BranchStatus status);

    Optional<Branch> findFirstByOrderByIdAsc();
  
    List<Branch> findAllByOrderByCreatedAtDesc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
    List<Branch> findByStatusOrderByNameAsc(BranchStatus status);
}


