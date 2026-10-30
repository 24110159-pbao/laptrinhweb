package example.com.ltw.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonResultDto {
    private Long categoryId;
    private String categoryName;
    private ProductCompareDto product1;
    private ProductCompareDto product2;

    /**
     * 1: Sản phẩm 1 lớn hơn / tốt hơn
     * 2: Sản phẩm 2 lớn hơn / tốt hơn
     * 0: Ngang nhau hoặc không có dữ liệu
     */
    private int screenSizeWinner;
    private int batteryWinner;
    private int priceWinner; // 1: Sản phẩm 1 giá tốt hơn (rẻ hơn), 2: Sản phẩm 2 rẻ hơn
}
