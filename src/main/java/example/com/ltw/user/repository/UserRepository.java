package example.com.ltw.user.repository;

import example.com.ltw.user.entity.User;
import example.com.ltw.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<User> findAllByBranch_IdAndRoleOrderByCreatedAtDesc(Long branchId, Role role);

    Optional<User> findByIdAndBranch_IdAndRole(Long id, Long branchId, Role role);

    long countByBranch_IdAndRole(Long branchId, Role role);
}
