package example.com.ltw.manager.service;

import example.com.ltw.branch.entity.Branch;
import example.com.ltw.catalog.entity.ProductVariantStatus;
import example.com.ltw.inventory.entity.BranchInventory;
import example.com.ltw.inventory.repository.BranchInventoryRepository;
import example.com.ltw.manager.dto.StaffForm;
import example.com.ltw.order.entity.Order;
import example.com.ltw.order.entity.OrderItem;
import example.com.ltw.order.repository.OrderItemRepository;
import example.com.ltw.order.repository.OrderRepository;
import example.com.ltw.user.entity.Role;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.entity.UserStatus;
import example.com.ltw.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerService {

    private final UserRepository userRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User getManager(Long managerId) {
        return requireManager(managerId);
    }

    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary(Long managerId) {
        Branch branch = requireManager(managerId).getBranch();
        Long branchId = branch.getId();

        return new DashboardSummary(
                userRepository.countByBranch_IdAndRole(branchId, Role.STAFF),
                branchInventoryRepository.countByBranch_Id(branchId),
                orderRepository.countByBranch_Id(branchId)
        );
    }

    @Transactional(readOnly = true)
    public List<BranchInventory> getInventory(Long managerId) {
        Long branchId = requireManager(managerId).getBranch().getId();
        return branchInventoryRepository.findAllByBranch_IdOrderByIdAsc(branchId);
    }

    @Transactional
    public void updateInventoryStatus(
            Long managerId,
            Long inventoryId,
            ProductVariantStatus status
    ) {
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái tồn kho không hợp lệ.");
        }

        Long branchId = requireManager(managerId).getBranch().getId();
        BranchInventory inventory = branchInventoryRepository
                .findByIdAndBranch_Id(inventoryId, branchId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy tồn kho thuộc chi nhánh của bạn."
                ));

        inventory.setStatus(status);
    }

    @Transactional(readOnly = true)
    public List<User> getStaff(Long managerId) {
        Long branchId = requireManager(managerId).getBranch().getId();
        return userRepository.findAllByBranch_IdAndRoleOrderByCreatedAtDesc(
                branchId,
                Role.STAFF
        );
    }

    @Transactional(readOnly = true)
    public User getStaff(Long managerId, Long staffId) {
        Long branchId = requireManager(managerId).getBranch().getId();
        return findStaffInBranch(staffId, branchId);
    }

    @Transactional
    public void createStaff(Long managerId, StaffForm form) {
        Branch branch = requireManager(managerId).getBranch();
        String email = normalizeEmail(form.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }
        if (form.getPassword() == null || form.getPassword().isBlank()
                || form.getPassword().length() < 8 || form.getPassword().length() > 72) {
            throw new IllegalArgumentException("Mật khẩu phải có từ 8 đến 72 ký tự.");
        }

        User staff = new User();
        staff.setFullName(form.getFullName().trim());
        staff.setEmail(email);
        staff.setPhone(normalizeOptional(form.getPhone()));
        staff.setPassword(passwordEncoder.encode(form.getPassword()));
        staff.setRole(Role.STAFF);
        staff.setStatus(UserStatus.ACTIVE);
        staff.setBranch(branch);
        userRepository.save(staff);
    }

    @Transactional
    public void updateStaff(Long managerId, Long staffId, StaffForm form) {
        Long branchId = requireManager(managerId).getBranch().getId();
        User staff = findStaffInBranch(staffId, branchId);
        String email = normalizeEmail(form.getEmail());
        if (!staff.getEmail().equalsIgnoreCase(email)
                && userRepository.existsByEmailAndIdNot(email, staffId)) {
            throw new DuplicateEmailException();
        }

        staff.setFullName(form.getFullName().trim());
        staff.setEmail(email);
        staff.setPhone(normalizeOptional(form.getPhone()));
    }

    @Transactional
    public UserStatus toggleStaffStatus(Long managerId, Long staffId) {
        Long branchId = requireManager(managerId).getBranch().getId();
        User staff = findStaffInBranch(staffId, branchId);
        UserStatus nextStatus = staff.getStatus() == UserStatus.LOCKED
                ? UserStatus.ACTIVE
                : UserStatus.LOCKED;
        staff.setStatus(nextStatus);
        return nextStatus;
    }

    @Transactional(readOnly = true)
    public List<Order> getOrders(Long managerId) {
        Long branchId = requireManager(managerId).getBranch().getId();
        return orderRepository.findAllByBranch_IdOrderByCreatedAtDesc(branchId);
    }

    @Transactional(readOnly = true)
    public OrderDetails getOrderDetails(Long managerId, Long orderId) {
        Long branchId = requireManager(managerId).getBranch().getId();
        Order order = orderRepository.findByIdAndBranch_Id(orderId, branchId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy hóa đơn thuộc chi nhánh của bạn."
                ));
        List<OrderItem> items = orderItemRepository.findAllByOrder_IdOrderByIdAsc(orderId);
        return new OrderDetails(order, items);
    }

    private User requireManager(Long managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Vui lòng đăng nhập lại."
                ));
        if (manager.getRole() != Role.MANAGER || manager.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản không có quyền quản lý chi nhánh."
            );
        }
        if (manager.getBranch() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản Manager chưa được gán chi nhánh."
            );
        }
        return manager;
    }

    private User findStaffInBranch(Long staffId, Long branchId) {
        return userRepository.findByIdAndBranch_IdAndRole(staffId, branchId, Role.STAFF)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Staff thuộc chi nhánh của bạn."
                ));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record DashboardSummary(long staffCount, long inventoryCount, long orderCount) {
    }

    public record OrderDetails(Order order, List<OrderItem> items) {
    }

    public static class DuplicateEmailException extends RuntimeException {
    }
}
