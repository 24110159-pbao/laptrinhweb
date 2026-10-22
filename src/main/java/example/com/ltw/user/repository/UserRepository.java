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

    List<User> findAllByOrderByCreatedAtDesc();

    List<User> findAllByRoleOrderByCreatedAtDesc(Role role);

    List<User> findAllByRole(Role role);

    @org.springframework.data.jpa.repository.Query("""
        SELECT u FROM User u
        WHERE (:role IS NULL OR u.role = :role)
          AND (
            :kw IS NULL OR :kw = ''
            OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR u.phone LIKE CONCAT('%', :kw, '%')
          )
        ORDER BY u.createdAt DESC
    """)
    List<User> searchUsers(
            @org.springframework.data.repository.query.Param("role") Role role,
            @org.springframework.data.repository.query.Param("kw") String kw
    );

    long countByRole(Role role);
}

