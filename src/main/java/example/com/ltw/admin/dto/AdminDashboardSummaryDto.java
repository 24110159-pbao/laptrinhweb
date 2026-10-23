package example.com.ltw.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardSummaryDto {
    private BigDecimal totalRevenue;
    private long totalOrders;
    private long totalUsers;
    private long totalBranches;
    private long totalProducts;
    private List<String> monthlyLabels;
    private List<BigDecimal> monthlyRevenues;
    private List<Long> monthlyOrders;
    private Map<String, Long> orderStatusCounts;
    private List<BranchRevenueDto> branchRevenues;
}
