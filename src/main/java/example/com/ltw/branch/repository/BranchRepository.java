package example.com.ltw.branch.repository;

import example.com.ltw.branch.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findAllByOrderByCreatedAtDesc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}