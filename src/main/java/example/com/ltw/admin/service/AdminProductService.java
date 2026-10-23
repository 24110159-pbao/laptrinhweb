package example.com.ltw.admin.service;

import example.com.ltw.admin.dto.AdminProductForm;
import example.com.ltw.admin.dto.AdminVariantForm;
import example.com.ltw.admin.dto.BranchInventoryForm;
import example.com.ltw.admin.dto.VariantInventoryDto;
import example.com.ltw.branch.entity.Branch;
import example.com.ltw.branch.repository.BranchRepository;
import example.com.ltw.catalog.entity.Brand;
import example.com.ltw.catalog.entity.Category;
import example.com.ltw.catalog.entity.Product;
import example.com.ltw.catalog.entity.ProductStatus;
import example.com.ltw.catalog.entity.ProductVariant;
import example.com.ltw.catalog.entity.ProductVariantStatus;
import example.com.ltw.catalog.repository.BrandRepository;
import example.com.ltw.catalog.repository.CategoryRepository;
import example.com.ltw.catalog.repository.ProductRepository;
import example.com.ltw.catalog.repository.ProductVariantRepository;
import example.com.ltw.inventory.entity.BranchInventory;
import example.com.ltw.inventory.repository.BranchInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final BranchRepository branchRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<Product> getProducts(String keyword, Long categoryId, Long brandId, ProductStatus status) {
        String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        if (kw == null && categoryId == null && brandId == null && status == null) {
            return productRepository.findAllByOrderByCreatedAtDesc();
        }
        return productRepository.filterProductsForAdmin(kw, categoryId, brandId, status);
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm."));
    }

    @Transactional(readOnly = true)
    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional
    public Product createProduct(AdminProductForm form) {
        String name = form.getName().trim();
        String slug = (form.getSlug() != null && !form.getSlug().trim().isEmpty())
                ? toSlug(form.getSlug().trim())
                : toSlug(name);

        if (productRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis() % 10000;
        }

        Brand brand = brandRepository.findById(form.getBrandId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thương hiệu không tồn tại."));

        Category category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục không tồn tại."));

        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setDescription(form.getDescription());
        product.setOperatingSystem(form.getOperatingSystem());
        product.setChipset(form.getChipset());
        product.setScreenSize(form.getScreenSize());
        product.setBatteryCapacity(form.getBatteryCapacity());
        product.setBrand(brand);
        product.setCategory(category);
        product.setStatus(form.getStatus() != null ? form.getStatus() : ProductStatus.ACTIVE);

        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, AdminProductForm form) {
        Product product = getProductById(id);
        String name = form.getName().trim();
        String slug = (form.getSlug() != null && !form.getSlug().trim().isEmpty())
                ? toSlug(form.getSlug().trim())
                : product.getSlug();

        if (!product.getSlug().equalsIgnoreCase(slug) && productRepository.existsBySlugAndIdNot(slug, id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug đã được sử dụng bởi sản phẩm khác.");
        }

        Brand brand = brandRepository.findById(form.getBrandId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thương hiệu không tồn tại."));

        Category category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục không tồn tại."));

        product.setName(name);
        product.setSlug(slug);
        product.setDescription(form.getDescription());
        product.setOperatingSystem(form.getOperatingSystem());
        product.setChipset(form.getChipset());
        product.setScreenSize(form.getScreenSize());
        product.setBatteryCapacity(form.getBatteryCapacity());
        product.setBrand(brand);
        product.setCategory(category);
        if (form.getStatus() != null) {
            product.setStatus(form.getStatus());
        }

        return productRepository.save(product);
    }

    @Transactional
    public ProductStatus toggleProductStatus(Long id) {
        Product product = getProductById(id);
        if (product.getStatus() == ProductStatus.ACTIVE) {
            product.setStatus(ProductStatus.INACTIVE);
        } else {
            product.setStatus(ProductStatus.ACTIVE);
        }
        productRepository.save(product);
        return product.getStatus();
    }

    // ==========================================
    // VARIANTS
    // ==========================================

    @Transactional(readOnly = true)
    public List<ProductVariant> getVariantsByProductId(Long productId) {
        return productVariantRepository.findByProductId(productId);
    }

    @Transactional
    public ProductVariant addVariant(Long productId, AdminVariantForm form) {
        Product product = getProductById(productId);

        String ram = form.getRam().trim();
        String storage = form.getStorage().trim();
        String color = form.getColor().trim();

        if (productVariantRepository.existsByProductIdAndRamAndStorageAndColor(productId, ram, storage, color)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Biến thể với cấu hình (" + ram + ", " + storage + ", " + color + ") đã tồn tại.");
        }

        if (form.getSalePrice() != null && form.getSalePrice().compareTo(form.getPrice()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá khuyến mãi không thể lớn hơn giá gốc.");
        }

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setRam(ram);
        variant.setStorage(storage);
        variant.setColor(color);
        variant.setPrice(form.getPrice());
        variant.setSalePrice(form.getSalePrice());
        variant.setStatus(form.getStatus() != null ? form.getStatus() : ProductVariantStatus.ACTIVE);

        return productVariantRepository.save(variant);
    }

    @Transactional
    public ProductVariant updateVariant(Long variantId, AdminVariantForm form) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biến thể."));

        Long productId = variant.getProduct().getId();
        String ram = form.getRam().trim();
        String storage = form.getStorage().trim();
        String color = form.getColor().trim();

        if (productVariantRepository.existsByProductIdAndRamAndStorageAndColorAndIdNot(productId, ram, storage, color, variantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cấu hình (" + ram + ", " + storage + ", " + color + ") đã tồn tại ở biến thể khác.");
        }

        if (form.getSalePrice() != null && form.getSalePrice().compareTo(form.getPrice()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá khuyến mãi không thể lớn hơn giá gốc.");
        }

        variant.setRam(ram);
        variant.setStorage(storage);
        variant.setColor(color);
        variant.setPrice(form.getPrice());
        variant.setSalePrice(form.getSalePrice());
        if (form.getStatus() != null) {
            variant.setStatus(form.getStatus());
        }

        return productVariantRepository.save(variant);
    }

    // ==========================================
    // ALLOCATION TO BRANCHES
    // ==========================================

    @Transactional(readOnly = true)
    public List<VariantInventoryDto> getVariantsForAllocation(Long productId, Long branchId) {
        List<ProductVariant> variants = productVariantRepository.findByProductId(productId);
        List<VariantInventoryDto> dtoList = new ArrayList<>();

        for (ProductVariant v : variants) {
            Optional<BranchInventory> invOpt = branchInventoryRepository.findByBranch_IdAndVariant_Id(branchId, v.getId());
            int currentStock = invOpt.map(BranchInventory::getQuantity).orElse(0);
            int reserved = invOpt.map(BranchInventory::getReservedQuantity).orElse(0);
            String variantName = v.getRam() + " / " + v.getStorage() + " - " + v.getColor();

            dtoList.add(new VariantInventoryDto(
                    v.getId(),
                    variantName,
                    v.getRam(),
                    v.getStorage(),
                    v.getColor(),
                    v.getPrice(),
                    currentStock,
                    reserved
            ));
        }

        return dtoList;
    }

    @Transactional
    public BranchInventory allocateBranchInventory(BranchInventoryForm form) {
        Branch branch = branchRepository.findById(form.getBranchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chi nhánh không tồn tại."));

        ProductVariant variant = productVariantRepository.findById(form.getVariantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Biến thể không tồn tại."));

        if (form.getQuantity() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng tồn kho không được âm.");
        }

        BranchInventory inventory = branchInventoryRepository
                .findByBranch_IdAndVariant_Id(branch.getId(), variant.getId())
                .orElseGet(() -> {
                    BranchInventory newInv = new BranchInventory();
                    newInv.setBranch(branch);
                    newInv.setVariant(variant);
                    newInv.setReservedQuantity(0);
                    return newInv;
                });

        if (form.getQuantity() < inventory.getReservedQuantity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Số lượng tổng (" + form.getQuantity() + ") không được nhỏ hơn số lượng đang giữ (reserved: "
                            + inventory.getReservedQuantity() + ").");
        }

        inventory.setQuantity(form.getQuantity());
        if (form.getStatus() != null) {
            inventory.setStatus(form.getStatus());
        }

        return branchInventoryRepository.save(inventory);
    }

    private String toSlug(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String noDiacritics = pattern.matcher(normalized).replaceAll("")
                .replace("đ", "d").replace("Đ", "d");
        return noDiacritics.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
