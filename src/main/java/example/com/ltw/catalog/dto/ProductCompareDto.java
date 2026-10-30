package example.com.ltw.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCompareDto {
    private Long id;
    private String name;
    private String slug;
    private Long categoryId;
    private String categoryName;
    private String brandName;
    private String imageUrl;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer discountPercent;
    private Integer totalStock;
    private boolean inStock;
    private String specsLine;
    private String operatingSystem;
    private String chipset;
    private BigDecimal screenSize;
    private Integer batteryCapacity;
    @Builder.Default
    private List<String> ramOptions = new ArrayList<>();
    @Builder.Default
    private List<String> storageOptions = new ArrayList<>();
    @Builder.Default
    private List<String> colorOptions = new ArrayList<>();
    private String description;

    public String getFormattedPrice() {
        if (price == null) return "Liên hệ";
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(price) + "₫";
    }

    public String getFormattedOriginalPrice() {
        if (originalPrice == null) return null;
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(originalPrice) + "₫";
    }

    public String getFormattedScreenSize() {
        if (screenSize == null) return "Đang cập nhật";
        return screenSize + " inch";
    }

    public String getFormattedBattery() {
        if (batteryCapacity == null || batteryCapacity == 0) return "Đang cập nhật";
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(batteryCapacity) + " mAh";
    }
}
