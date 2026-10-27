package example.com.ltw.catalog.service;

import example.com.ltw.catalog.dto.CategoryCompareDto;
import example.com.ltw.catalog.dto.ComparisonResultDto;
import example.com.ltw.catalog.dto.ProductCompareDto;
import example.com.ltw.catalog.entity.*;
import example.com.ltw.catalog.repository.CategoryRepository;
import example.com.ltw.catalog.repository.ProductRepository;
import example.com.ltw.inventory.repository.BranchInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompareServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BranchInventoryRepository inventoryRepository;

    private CompareService compareService;

    private Category phoneCategory;
    private Category laptopCategory;
    private Brand appleBrand;
    private Brand samsungBrand;

    @BeforeEach
    void setUp() {
        compareService = new CompareService(productRepository, categoryRepository, inventoryRepository);

        phoneCategory = new Category();
        phoneCategory.setId(1L);
        phoneCategory.setName("Điện thoại");
        phoneCategory.setStatus(CategoryStatus.ACTIVE);

        laptopCategory = new Category();
        laptopCategory.setId(2L);
        laptopCategory.setName("Laptop");
        laptopCategory.setStatus(CategoryStatus.ACTIVE);

        appleBrand = new Brand();
        appleBrand.setId(10L);
        appleBrand.setName("Apple");

        samsungBrand = new Brand();
        samsungBrand.setId(20L);
        samsungBrand.setName("Samsung");
    }

    @Test
    void getCompareCategories_returnsActiveCategoriesWithCounts() {
        when(categoryRepository.findAll()).thenReturn(List.of(phoneCategory, laptopCategory));

        Product p1 = createProduct(1L, "iPhone 16 Pro Max", phoneCategory, appleBrand);
        Product p2 = createProduct(2L, "Galaxy S24 Ultra", phoneCategory, samsungBrand);
        when(productRepository.findByStatus(ProductStatus.ACTIVE)).thenReturn(List.of(p1, p2));

        List<CategoryCompareDto> categories = compareService.getCompareCategories();

        assertEquals(1, categories.size());
        assertEquals("Điện thoại", categories.get(0).getName());
        assertEquals(2L, categories.get(0).getProductCount());
    }

    @Test
    void compareTwoProducts_sameCategory_succeedsWithMetrics() {
        Product p1 = createProduct(1L, "iPhone 16 Pro Max", phoneCategory, appleBrand);
        p1.setScreenSize(new BigDecimal("6.9"));
        p1.setBatteryCapacity(4685);
        p1.setVariants(List.of(createVariant(101L, p1, new BigDecimal("34990000"), null, "8GB", "256GB", "Titan")));

        Product p2 = createProduct(2L, "Galaxy S24 Ultra", phoneCategory, samsungBrand);
        p2.setScreenSize(new BigDecimal("6.8"));
        p2.setBatteryCapacity(5000);
        p2.setVariants(List.of(createVariant(102L, p2, new BigDecimal("31990000"), null, "12GB", "256GB", "Xám Titan")));

        when(productRepository.findByIdAndStatus(1L, ProductStatus.ACTIVE)).thenReturn(Optional.of(p1));
        when(productRepository.findByIdAndStatus(2L, ProductStatus.ACTIVE)).thenReturn(Optional.of(p2));

        ComparisonResultDto result = compareService.compareTwoProducts(1L, 2L);

        assertNotNull(result);
        assertEquals(1L, result.getCategoryId());
        assertEquals("Điện thoại", result.getCategoryName());
        assertEquals(1, result.getScreenSizeWinner()); // p1 has 6.9" > 6.8"
        assertEquals(2, result.getBatteryWinner());    // p2 has 5000mAh > 4685mAh
        assertEquals(2, result.getPriceWinner());      // p2 is cheaper (31.9M < 34.9M)
    }

    @Test
    void compareTwoProducts_differentCategory_throwsIllegalArgumentException() {
        Product phone = createProduct(1L, "iPhone 16 Pro Max", phoneCategory, appleBrand);
        phone.setVariants(new ArrayList<>());

        Product laptop = createProduct(3L, "MacBook Pro M3", laptopCategory, appleBrand);
        laptop.setVariants(new ArrayList<>());

        when(productRepository.findByIdAndStatus(1L, ProductStatus.ACTIVE)).thenReturn(Optional.of(phone));
        when(productRepository.findByIdAndStatus(3L, ProductStatus.ACTIVE)).thenReturn(Optional.of(laptop));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                compareService.compareTwoProducts(1L, 3L)
        );

        assertTrue(ex.getMessage().contains("cùng loại"));
    }

    private Product createProduct(Long id, String name, Category cat, Brand brand) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setSlug("slug-" + id);
        p.setCategory(cat);
        p.setBrand(brand);
        p.setStatus(ProductStatus.ACTIVE);
        p.setVariants(new ArrayList<>());
        p.setImages(new ArrayList<>());
        return p;
    }

    private ProductVariant createVariant(Long id, Product p, BigDecimal price, BigDecimal salePrice, String ram, String storage, String color) {
        ProductVariant v = new ProductVariant();
        v.setId(id);
        v.setProduct(p);
        v.setPrice(price);
        v.setSalePrice(salePrice);
        v.setRam(ram);
        v.setStorage(storage);
        v.setColor(color);
        v.setStatus(ProductVariantStatus.ACTIVE);
        return v;
    }
}
