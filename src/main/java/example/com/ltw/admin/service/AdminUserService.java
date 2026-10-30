package example.com.ltw.admin.service;

import example.com.ltw.admin.dto.AdminUserForm;
import example.com.ltw.auth.service.AuthService;
import example.com.ltw.branch.entity.Branch;
import example.com.ltw.branch.repository.BranchRepository;
import example.com.ltw.user.entity.Role;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.entity.UserStatus;
import example.com.ltw.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    public User requireAdmin(HttpSession session) {
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        }
        Object obj = session.getAttribute(AuthService.SESSION_CURRENT_USER);
        if (!(obj instanceof User user) || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        }
        User dbUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại."));
        if (dbUser.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập khu vực Admin.");
        }
        return dbUser;
    }

    @Transactional(readOnly = true)
    public List<User> getUsers(Role role, String keyword) {
        String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        if (role == null && kw == null) {
            return userRepository.findAllByOrderByCreatedAtDesc();
        }
        return userRepository.searchUsers(role, kw);
    }

    @Transactional(readOnly = true)
    public List<User> getManagers() {
        return userRepository.findAllByRoleOrderByCreatedAtDesc(Role.MANAGER);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));
    }

    @Transactional
    public User createManager(AdminUserForm form) {
        String email = form.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email này đã được sử dụng.");
        }

        if (form.getPassword() == null || form.getPassword().trim().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu cho Manager phải từ 6 ký tự trở lên.");
        }

        if (form.getBranchId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn chi nhánh cho Manager.");
        }

        Branch branch = branchRepository.findById(form.getBranchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chi nhánh không tồn tại."));

        User manager = new User();
        manager.setFullName(form.getFullName().trim());
        manager.setEmail(email);
        manager.setPhone(form.getPhone() != null ? form.getPhone().trim() : null);
        manager.setPassword(passwordEncoder.encode(form.getPassword().trim()));
        manager.setRole(Role.MANAGER);
        manager.setStatus(form.getStatus() != null ? form.getStatus() : UserStatus.ACTIVE);
        manager.setEmailVerified(true);
        manager.setBranch(branch);

        return userRepository.save(manager);
    }

    @Transactional
    public User updateManager(Long id, AdminUserForm form) {
        User manager = getUserById(id);
        if (manager.getRole() != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Người dùng này không phải là Manager.");
        }

        String email = form.getEmail().trim().toLowerCase();
        if (!manager.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmailAndIdNot(email, id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email này đã được sử dụng bởi tài khoản khác.");
        }

        if (form.getBranchId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn chi nhánh cho Manager.");
        }

        Branch branch = branchRepository.findById(form.getBranchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chi nhánh không tồn tại."));

        manager.setFullName(form.getFullName().trim());
        manager.setEmail(email);
        manager.setPhone(form.getPhone() != null ? form.getPhone().trim() : null);
        manager.setBranch(branch);

        if (form.getStatus() != null) {
            manager.setStatus(form.getStatus());
        }

        // Đổi mật khẩu nếu có nhập mật khẩu mới
        if (form.getPassword() != null && !form.getPassword().trim().isEmpty()) {
            if (form.getPassword().trim().length() < 6) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới phải từ 6 ký tự trở lên.");
            }
            manager.setPassword(passwordEncoder.encode(form.getPassword().trim()));
        }

        return userRepository.save(manager);
    }

    @Transactional
    public UserStatus toggleUserStatus(Long id, Long currentAdminId) {
        if (id.equals(currentAdminId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể tự khóa tài khoản Admin của chính bạn.");
        }

        User user = getUserById(id);
        if (user.getStatus() == UserStatus.ACTIVE) {
            user.setStatus(UserStatus.LOCKED);
        } else {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);
        return user.getStatus();
    }
}
