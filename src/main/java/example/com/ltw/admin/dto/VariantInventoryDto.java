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
public class VariantInventoryDto {
    private Long variantId;
    private String variantName;
    private String ram;
    private String storage;
    private String color;
    private BigDecimal price;
    private int currentBranchStock;
    private int reservedQuantity;
}
