package example.com.ltw.user.repository;

import example.com.ltw.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    java.util.List<User> findAllByBranch_IdAndRoleOrderByCreatedAtDesc(Long branchId, example.com.ltw.user.entity.Role role);

    Optional<User> findByIdAndBranch_IdAndRole(Long id, Long branchId, example.com.ltw.user.entity.Role role);

    long countByBranch_IdAndRole(Long branchId, example.com.ltw.user.entity.Role role);
}
