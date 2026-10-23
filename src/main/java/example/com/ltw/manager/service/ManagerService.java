
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
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class ManagerService {

    private final UserRepository userRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PasswordEncoder passwordEncoder;

    // =========================
    // MANAGER
    // =========================

    @Transactional(readOnly = true)
    public User getManager(Long managerId) {
        return requireManager(managerId);
    }

    // =========================
    // DASHBOARD
    // =========================

    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary(Long managerId) {
        Branch branch = requireManager(managerId).getBranch();
        Long branchId = branch.getId();

        long staffCount =
                userRepository.countByBranch_IdAndRole(
                        branchId,
                        Role.STAFF
                );

        long inventoryCount =
                branchInventoryRepository.countByBranch_Id(branchId);

        long orderCount =
                orderRepository.countByBranch_Id(branchId);

        return new DashboardSummary(
                staffCount,
                inventoryCount,
                orderCount
        );
    }

    // =========================
    // INVENTORY
    // =========================

    @Transactional(readOnly = true)
    public List<BranchInventory> getInventory(Long managerId) {
        Long branchId = requireManager(managerId)
                .getBranch()
                .getId();

        return branchInventoryRepository
                .findAllByBranch_IdOrderByIdAsc(branchId);
    }

    @Transactional
    public void updateInventoryStatus(
            Long managerId,
            Long inventoryId,
            ProductVariantStatus status
    ) {
        if (status == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trạng thái tồn kho không hợp lệ."
            );
        }

        Long branchId = requireManager(managerId)
                .getBranch()
                .getId();

        BranchInventory inventory = branchInventoryRepository
                .findByIdAndBranch_Id(inventoryId, branchId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy tồn kho thuộc chi nhánh của bạn."
                ));

        inventory.setStatus(status);
    }

    // =========================
    // STAFF
    // =========================

    @Transactional(readOnly = true)
    public List<User> getStaff(Long managerId) {
        Long branchId = requireManager(managerId)
                .getBranch()
                .getId();

        return userRepository
                .findAllByBranch_IdAndRoleOrderByCreatedAtDesc(
                        branchId,
                        Role.STAFF
                );
    }

    @Transactional(readOnly = true)
    public User getStaff(Long managerId, Long staffId) {
        Long branchId = requireManager(managerId)
                .getBranch()
                .getId();

        return findStaffInBranch(staffId, branchId);
    }

    @Transactional
    public void createStaff(Long managerId, StaffForm form) {
        validateStaffForm(form, true);

        Branch branch = requireManager(managerId).getBranch();

        String email = normalizeEmail(form.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        User staff = new User();
        staff.setFullName(form.getFullName().trim());
        staff.setEmail(email);
        staff.setPhone(normalizeOptional(form.getPhone()));
        staff.setPassword(
                passwordEncoder.encode(form.getPassword())
        );
        staff.setRole(Role.STAFF);
        staff.setStatus(UserStatus.ACTIVE);
        staff.setBranch(branch);

        userRepository.save(staff);
    }

    @Transactional
    public void updateStaff(
            Long managerId,
            Long staffId,
            StaffForm form
    ) {
        validateStaffForm(form, false);

        Long branchId = requireManager(managerId)
                .getBranch()
                .getId();

        User staff = findStaffInBranch(staffId, branchId);
        String email = normalizeEmail(form.getEmail());

        if (!staff.getEmail().equalsIgnoreCase(email)
                && userRepository.existsByEmailAndIdNot(email, staffId)) {
            throw new DuplicateEmailException();
        }

        staff.setFullName(form.getFullName().trim());
        staff.setEmail(email);
        staff.setPhone(normalizeOptional(form.getPhone()));

        // Không thay đổi mật khẩu khi cập nhật thông tin Staff.
        // Chức năng đổi mật khẩu nên được xử lý riêng.
    }

    @Transactional
    public UserStatus toggleStaffStatus(
            Long managerId,
            Long staffId
    ) {
        Long branchId = requireManager(managerId)
                .getBranch()
                .getId();

        User staff = findStaffInBranch(staffId, branchId);

        UserStatus currentStatus = staff.getStatus();

        if (currentStatus == UserStatus.LOCKED) {
            staff.setStatus(UserStatus.ACTIVE);
        } else if (currentStatus == UserStatus.ACTIVE) {
            staff.setStatus(UserStatus.LOCKED);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể thay đổi trạng thái tài khoản Staff này."
            );
        }

        return staff.getStatus();
    }
    // =========================
    // CUSTOMERS
    // =========================

    @Transactional(readOnly = true)
    public List<User> getCustomers(Long managerId) {
        Long branchId = requireManager(managerId)
                .getBranch()
                .getId();

        return orderRepository.findCustomersByBranchId(branchId);
    }


    @Transactional(readOnly = true)
    public Map<String, Long> getOrderStatusCounts(Long managerId) {
        Long branchId = requireManager(managerId)
                .getBranch()
                .getId();

        Map<String, Long> counts = new HashMap<>();

        List<Object[]> results =
                orderRepository.countOrdersByStatus(branchId);

        for (Object[] row : results) {
            String status = row[0] == null
                    ? "UNKNOWN"
                    : row[0].toString();

            Long count = ((Number) row[1]).longValue();

            counts.put(status, count);
        }

        return counts;
    }
    // =========================
    // ORDERS
    // =========================

    @Transactional(readOnly = true)
    public List<Order> getOrders(Long managerId) {
        Long branchId = requireManager(managerId)
                .getBranch()
                .getId();

        return orderRepository
                .findAllByBranch_IdOrderByCreatedAtDesc(branchId);
    }

    @Transactional(readOnly = true)
    public OrderDetails getOrderDetails(
            Long managerId,
            Long orderId
    ) {
        Long branchId = requireManager(managerId)
                .getBranch()
                .getId();

        Order order = orderRepository
                .findByIdAndBranch_Id(orderId, branchId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy hóa đơn thuộc chi nhánh của bạn."
                ));

        List<OrderItem> items = orderItemRepository
                .findAllByOrder_IdOrderByIdAsc(order.getId());

        return new OrderDetails(order, items);
    }

    // =========================
    // VALIDATION
    // =========================

    private void validateStaffForm(
            StaffForm form,
            boolean creating
    ) {
        if (form == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thông tin Staff không hợp lệ."
            );
        }

        if (form.getFullName() == null
                || form.getFullName().isBlank()
                || form.getFullName().trim().length() > 150) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Họ tên không hợp lệ."
            );
        }

        if (form.getEmail() == null
                || form.getEmail().isBlank()
                || form.getEmail().trim().length() > 150
                || !form.getEmail().trim().matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email không hợp lệ."
            );
        }

        String phone = form.getPhone();

        if (phone == null || !phone.trim().matches("^0[0-9]{9}$")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0."
            );
        }

        String password = form.getPassword();

        if (creating) {
            if (password == null || password.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Vui lòng nhập mật khẩu ban đầu."
                );
            }

            validatePassword(password);
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 20) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mật khẩu phải có từ 6 đến 20 ký tự."
            );
        }
    }

    // =========================
    // ACCESS CONTROL
    // =========================

    private User requireManager(Long managerId) {
        if (managerId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Vui lòng đăng nhập lại."
            );
        }

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Vui lòng đăng nhập lại."
                ));

        if (manager.getRole() != Role.MANAGER
                || manager.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản không có quyền quản lý chi nhánh."
            );
        }

        if (manager.getBranch() == null
                || manager.getBranch().getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản Manager chưa được gán chi nhánh."
            );
        }

        return manager;
    }

    private User findStaffInBranch(
            Long staffId,
            Long branchId
    ) {
        if (staffId == null || branchId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thông tin Staff không hợp lệ."
            );
        }

        return userRepository
                .findByIdAndBranch_IdAndRole(
                        staffId,
                        branchId,
                        Role.STAFF
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Staff thuộc chi nhánh của bạn."
                ));
    }

    // =========================
    // NORMALIZATION
    // =========================

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email không được để trống."
            );
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    // =========================
    // DTO RECORDS
    // =========================

    public record DashboardSummary(
            long staffCount,
            long inventoryCount,
            long orderCount
    ) {
    }

    public record OrderDetails(
            Order order,
            List<OrderItem> items
    ) {
    }

    // =========================
    // CUSTOM EXCEPTION
    // =========================

    public static class DuplicateEmailException
            extends RuntimeException {
    }

    @Transactional(readOnly = true)
    public DashboardAnalytics getDashboardAnalytics(Long managerId) {
        Long branchId = requireManager(managerId)
                .getBranch()
                .getId();

        // Tổng doanh thu từ các đơn đã giao
        BigDecimal revenue = orderRepository
                .sumDeliveredRevenueByBranchId(branchId);

        if (revenue == null) {
            revenue = BigDecimal.ZERO;
        }

        // Thống kê đơn hàng theo trạng thái
        Map<String, Long> statusCounts =
                orderRepository.countOrdersByStatus(branchId)
                        .stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        row -> row[0] == null
                                                ? "UNKNOWN"
                                                : row[0].toString(),
                                        row -> ((Number) row[1]).longValue(),
                                        Long::sum,
                                        java.util.LinkedHashMap::new
                                )
                        );

        long pendingOrderCount =
                statusCounts.getOrDefault("PENDING", 0L)
                        + statusCounts.getOrDefault("CONFIRMED", 0L);

        // Lấy dữ liệu doanh thu 12 tháng gần nhất,
        // bao gồm tháng hiện tại.
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime fromDate = currentMonth
                .minusMonths(11)
                .atDay(1)
                .atStartOfDay();

        LocalDateTime toDate = currentMonth
                .plusMonths(1)
                .atDay(1)
                .atStartOfDay();

        List<Object[]> monthlyRows =
                orderRepository.findMonthlyDeliveredRevenue(
                        branchId,
                        fromDate,
                        toDate
                );


        Map<YearMonth, BigDecimal> monthlyRevenueMap =
                new java.util.HashMap<>();

        for (Object[] row : monthlyRows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            BigDecimal amount = row[2] instanceof BigDecimal value
                    ? value
                    : new BigDecimal(row[2].toString());

            monthlyRevenueMap.put(
                    YearMonth.of(year, month),
                    amount
            );
        }
        List<Object[]> monthlyOrderRows =
                orderRepository.findMonthlyOrderCounts(
                        branchId,
                        fromDate,
                        toDate
                );
        Map<YearMonth, Long> monthlyOrderMap = new java.util.HashMap<>();

        for (Object[] row : monthlyOrderRows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();

            monthlyOrderMap.put(
                    YearMonth.of(year, month),
                    count
            );
        }
        List<String> monthlyRevenueLabels = new ArrayList<>();
        List<BigDecimal> monthlyRevenueValues = new ArrayList<>();
        List<Long> monthlyOrderValues = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            YearMonth month = currentMonth.minusMonths(11 - i);

            monthlyRevenueLabels.add(
                    String.format(
                            "%02d/%d",
                            month.getMonthValue(),
                            month.getYear()
                    )
            );

            monthlyRevenueValues.add(
                    monthlyRevenueMap.getOrDefault(
                            month,
                            BigDecimal.ZERO
                    )
            );
            monthlyOrderValues.add(
                    monthlyOrderMap.getOrDefault(month, 0L)
            );
        }

        return new DashboardAnalytics(
                revenue,
                pendingOrderCount,
                statusCounts,
                monthlyRevenueLabels,
                monthlyRevenueValues,
                monthlyOrderValues
        );
    }

    public record DashboardAnalytics(
            BigDecimal revenue,
            long pendingOrderCount,
            Map<String, Long> orderStatusCounts,
            List<String> monthlyRevenueLabels,
            List<BigDecimal> monthlyRevenueValues,
            List<Long> monthlyOrderValues
    ) {
    }


}
