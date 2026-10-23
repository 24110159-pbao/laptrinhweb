package example.com.ltw.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BranchRevenueDto {
    private Long branchId;
    private String branchName;
    private String branchAddress;
    private BigDecimal revenue;
    private Long orderCount;
}
