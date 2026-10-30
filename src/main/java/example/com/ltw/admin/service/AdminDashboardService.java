package example.com.ltw.admin.service;

import example.com.ltw.admin.dto.AdminDashboardSummaryDto;
import example.com.ltw.admin.dto.BranchRevenueDto;
import example.com.ltw.branch.entity.Branch;
import example.com.ltw.branch.repository.BranchRepository;
import example.com.ltw.catalog.repository.ProductRepository;
import example.com.ltw.order.repository.OrderRepository;
import example.com.ltw.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public AdminDashboardSummaryDto getDashboardSummary() {
        BigDecimal totalRevenue = orderRepository.sumAllDeliveredRevenue();
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        long totalOrders = orderRepository.count();
        long totalUsers = userRepository.count();
        long totalBranches = branchRepository.count();
        long totalProducts = productRepository.count();

        // 1. Order Status Counts
        Map<String, Long> statusCounts = orderRepository.countAllOrdersByStatus()
                .stream()
                .collect(
                        java.util.stream.Collectors.toMap(
                                row -> row[0] == null ? "UNKNOWN" : row[0].toString(),
                                row -> ((Number) row[1]).longValue(),
                                Long::sum,
                                LinkedHashMap::new
                        )
                );

        // 2. 12 Months Analytics
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime fromDate = currentMonth.minusMonths(11).atDay(1).atStartOfDay();
        LocalDateTime toDate = currentMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<Object[]> monthlyRevenueRows = orderRepository.findAllMonthlyDeliveredRevenue(fromDate, toDate);
        Map<YearMonth, BigDecimal> monthlyRevenueMap = new HashMap<>();
        for (Object[] row : monthlyRevenueRows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            BigDecimal amount = row[2] instanceof BigDecimal value
                    ? value
                    : new BigDecimal(row[2].toString());
            monthlyRevenueMap.put(YearMonth.of(year, month), amount);
        }

        List<Object[]> monthlyOrderRows = orderRepository.findAllMonthlyOrderCounts(fromDate, toDate);
        Map<YearMonth, Long> monthlyOrderMap = new HashMap<>();
        for (Object[] row : monthlyOrderRows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();
            monthlyOrderMap.put(YearMonth.of(year, month), count);
        }

        List<String> monthlyLabels = new ArrayList<>();
        List<BigDecimal> monthlyRevenues = new ArrayList<>();
        List<Long> monthlyOrders = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            YearMonth month = currentMonth.minusMonths(11 - i);
            monthlyLabels.add(String.format("%02d/%d", month.getMonthValue(), month.getYear()));
            monthlyRevenues.add(monthlyRevenueMap.getOrDefault(month, BigDecimal.ZERO));
            monthlyOrders.add(monthlyOrderMap.getOrDefault(month, 0L));
        }

        // 3. Branch Revenues
        List<BranchRevenueDto> branchRevenues = getBranchRevenues();

        return new AdminDashboardSummaryDto(
                totalRevenue,
                totalOrders,
                totalUsers,
                totalBranches,
                totalProducts,
                monthlyLabels,
                monthlyRevenues,
                monthlyOrders,
                statusCounts,
                branchRevenues
        );
    }

    @Transactional(readOnly = true)
    public List<BranchRevenueDto> getBranchRevenues() {
        List<Branch> allBranches = branchRepository.findAllByOrderByCreatedAtDesc();
        List<Object[]> revRows = orderRepository.findRevenueByBranch();

        Map<Long, Object[]> revMap = new HashMap<>();
        for (Object[] row : revRows) {
            Long bId = ((Number) row[0]).longValue();
            revMap.put(bId, row);
        }

        List<BranchRevenueDto> result = new ArrayList<>();
        for (Branch b : allBranches) {
            Object[] row = revMap.get(b.getId());
            BigDecimal revenue = BigDecimal.ZERO;
            long orders = 0L;
            if (row != null) {
                revenue = row[2] instanceof BigDecimal val ? val : new BigDecimal(row[2].toString());
                orders = ((Number) row[3]).longValue();
            }
            result.add(new BranchRevenueDto(
                    b.getId(),
                    b.getName(),
                    b.getAddress(),
                    revenue,
                    orders
            ));
        }

        // Sort descending by revenue
        result.sort((a, b) -> b.getRevenue().compareTo(a.getRevenue()));
        return result;
    }
}
