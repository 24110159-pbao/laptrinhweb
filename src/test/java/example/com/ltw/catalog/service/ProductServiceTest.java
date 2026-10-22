package example.com.ltw.catalog.service;

import example.com.ltw.branch.entity.BranchStatus;
import example.com.ltw.branch.entity.Branch;
import example.com.ltw.branch.repository.BranchRepository;
import example.com.ltw.catalog.dto.ProductCardDto;
import example.com.ltw.catalog.entity.Brand;
import example.com.ltw.catalog.entity.Category;
import example.com.ltw.catalog.entity.Product;
import example.com.ltw.catalog.entity.ProductStatus;
import example.com.ltw.catalog.entity.ProductVariant;
import example.com.ltw.catalog.entity.ProductVariantStatus;
import example.com.ltw.catalog.repository.ProductRepository;
import example.com.ltw.inventory.entity.BranchInventory;
import example.com.ltw.inventory.repository.BranchInventoryRepository;
import example.com.ltw.order.entity.OrderStatus;
import example.com.ltw.order.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private BranchInventoryRepository inventoryRepository;
    @Mock
    private BranchRepository branchRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productRepository, orderItemRepository, inventoryRepository, branchRepository);
    }

    @Test
    void filtersByNormalizedNameCategoryBrandInclusivePriceAndStock() {
        Product samsung = product(1L, "Điện thoại Samsung", 10L, 20L, "100", "80");
        Product apple = product(2L, "Điện thoại Apple", 11L, 21L, "100", null);
        Product headset = product(3L, "Tai nghe Samsung", 10L, 20L, "80", null);
        BranchInventory samsungStock = inventory(samsung.getVariants().get(0), 4, 1);
        BranchInventory headsetStock = inventory(headset.getVariants().get(0), 0, 0);
        configureCatalog(List.of(samsung, apple, headset), List.of(samsungStock, headsetStock));

        Page<example.com.ltw.catalog.dto.ProductCardDto> result = productService.searchProducts(
                "DIEN thoai", List.of(10L), List.of(20L, 21L),
                new BigDecimal("80"), new BigDecimal("80"), "in", "price_asc", 1);

        assertEquals(1, result.getTotalElements());
        assertEquals(samsung.getId(), result.getContent().get(0).getId());
        assertEquals(new BigDecimal("80"), result.getContent().get(0).getPrice());
        assertEquals(3, result.getContent().get(0).getTotalStock());
    }

    @Test
    void keepsProductsUniqueAndLimitsCatalogPagesToTenItems() {
        List<Product> products = new ArrayList<>();
        for (long id = 1; id <= 12; id++) {
            products.add(product(id, "Điện thoại " + id, 10L, 20L, "100", null));
        }
        configureCatalog(products, List.of());

        Page<example.com.ltw.catalog.dto.ProductCardDto> firstPage = productService.searchProducts(
                null, List.of(), List.of(), null, null, "", "newest", 1);
        Page<example.com.ltw.catalog.dto.ProductCardDto> lastPage = productService.searchProducts(
                null, List.of(), List.of(), null, null, "", "newest", 9);

        assertEquals(10, firstPage.getContent().size());
        assertEquals(12, firstPage.getTotalElements());
        assertEquals(2, lastPage.getContent().size());
        assertEquals(2, lastPage.getNumber() + 1);
    }

    @Test
    void productDetailsUseFirstActiveVariantAndAggregateAvailableStockAcrossBranches() {
        Product product = product(4L, "Điện thoại", 10L, 20L, "100", "80");
        ProductVariant secondVariant = new ProductVariant();
        secondVariant.setId(42L);
        secondVariant.setProduct(product);
        secondVariant.setRam("12 GB");
        secondVariant.setStorage("256 GB");
        secondVariant.setColor("Blue");
        secondVariant.setPrice(new BigDecimal("120"));
        secondVariant.setSalePrice(new BigDecimal("110"));
        secondVariant.setImageUrl("/images/blue-phone.png");
        secondVariant.setStatus(ProductVariantStatus.ACTIVE);
        product.setVariants(List.of(secondVariant, product.getVariants().get(0)));

        Branch firstBranch = branch(1L, "Chi nhánh 1");
        Branch secondBranch = branch(2L, "Chi nhánh 2");
        ProductVariant firstVariant = product.getVariants().get(1);
        BranchInventory firstInventory = inventory(firstBranch, firstVariant, 5, 2);
        BranchInventory secondInventory = inventory(secondBranch, firstVariant, 3, 1);

        when(productRepository.findBySlugAndStatus("product-4", ProductStatus.ACTIVE))
                .thenReturn(Optional.of(product));
        when(branchRepository.findByStatusOrderByNameAsc(BranchStatus.ACTIVE))
                .thenReturn(List.of(firstBranch, secondBranch));
        when(inventoryRepository.findCatalogInventory(
                BranchStatus.ACTIVE, ProductVariantStatus.ACTIVE, ProductStatus.ACTIVE))
                .thenReturn(List.of(firstInventory, secondInventory));

        ProductCardDto result = productService.getProductDetailsBySlug("product-4").orElseThrow();

        assertEquals(firstVariant.getId(), result.getVariants().get(0).id());
        assertEquals(5, result.getVariants().get(0).totalStock());
        assertEquals(new BigDecimal("80"), result.getPrice());
        assertEquals(new BigDecimal("100"), result.getOriginalPrice());
        assertEquals(5, result.getTotalStock());
        assertEquals(2, result.getBranchStocks().size());
        assertEquals(firstInventory.getId(), result.getBranchStocks().get(0).variants().get(0).inventoryId());
        assertNull(result.getBranchStocks().get(1).variants().get(1).inventoryId());
        assertEquals("/images/blue-phone.png", result.getVariants().get(1).imageUrl());
    }

    private void configureCatalog(List<Product> products, List<BranchInventory> inventories) {
        when(productRepository.findByStatus(ProductStatus.ACTIVE)).thenReturn(products);
        when(orderItemRepository.findSalesByProduct(OrderStatus.DELIVERED)).thenReturn(List.of());
        when(inventoryRepository.findCatalogInventory(
                BranchStatus.ACTIVE, ProductVariantStatus.ACTIVE, ProductStatus.ACTIVE))
                .thenReturn(inventories);
    }

    private Product product(
            Long id, String name, Long categoryId, Long brandId, String price, String salePrice
    ) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setSlug("product-" + id);
        product.setStatus(ProductStatus.ACTIVE);
        product.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0).plusDays(id));

        Category category = new Category();
        category.setId(categoryId);
        category.setName("Category " + categoryId);
        product.setCategory(category);

        Brand brand = new Brand();
        brand.setId(brandId);
        brand.setName("Brand " + brandId);
        product.setBrand(brand);

        ProductVariant variant = new ProductVariant();
        variant.setId(id * 10);
        variant.setStatus(ProductVariantStatus.ACTIVE);
        variant.setRam("8 GB");
        variant.setStorage("128 GB");
        variant.setColor("Black");
        variant.setPrice(new BigDecimal(price));
        variant.setSalePrice(salePrice == null ? null : new BigDecimal(salePrice));
        variant.setProduct(product);
        product.setVariants(List.of(variant));
        return product;
    }

    private BranchInventory inventory(ProductVariant variant, int quantity, int reservedQuantity) {
        BranchInventory inventory = new BranchInventory();
        inventory.setVariant(variant);
        inventory.setQuantity(quantity);
        inventory.setReservedQuantity(reservedQuantity);
        return inventory;
    }

    private Branch branch(Long id, String name) {
        Branch branch = new Branch();
        branch.setId(id);
        branch.setName(name);
        branch.setAddress("Địa chỉ " + name);
        branch.setPhone("0123456789");
        branch.setStatus(BranchStatus.ACTIVE);
        return branch;
    }

    private BranchInventory inventory(
            Branch branch, ProductVariant variant, int quantity, int reservedQuantity
    ) {
        BranchInventory inventory = inventory(variant, quantity, reservedQuantity);
        inventory.setBranch(branch);
        inventory.setId(branch.getId() * 10);
        return inventory;
    }
}
