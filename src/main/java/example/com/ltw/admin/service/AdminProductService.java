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
import example.com.ltw.catalog.entity.ProductImage;
import example.com.ltw.catalog.entity.ProductStatus;
import example.com.ltw.catalog.entity.ProductVariant;
import example.com.ltw.catalog.entity.ProductVariantStatus;
import example.com.ltw.catalog.repository.BrandRepository;
import example.com.ltw.catalog.repository.CategoryRepository;
import example.com.ltw.catalog.repository.ProductImageRepository;
import example.com.ltw.catalog.repository.ProductRepository;
import example.com.ltw.catalog.repository.ProductVariantRepository;
import example.com.ltw.inventory.entity.BranchInventory;
import example.com.ltw.inventory.repository.BranchInventoryRepository;
import example.com.ltw.admin.dto.AdminBrandForm;
import example.com.ltw.admin.dto.AdminCategoryForm;
import example.com.ltw.catalog.entity.BrandStatus;
import example.com.ltw.catalog.entity.CategoryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
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
    private final ProductImageRepository productImageRepository;

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

    @Transactional(readOnly = true)
    public Brand getBrandById(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thương hiệu."));
    }

    @Transactional(readOnly = true)
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục."));
    }

    @Transactional
    public Product createProduct(AdminProductForm form) {
        return createProduct(form, List.of());
    }

    @Transactional
    public Product createProduct(
            AdminProductForm form,
            List<CloudinaryImageService.UploadedImage> uploadedImages
    ) {
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

        Product saved = productRepository.save(product);
        List<ProductImage> images = new ArrayList<>();
        List<CloudinaryImageService.UploadedImage> safeUploads =
                uploadedImages == null ? List.of() : uploadedImages;
        if (safeUploads.size() > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mỗi sản phẩm được có tối đa 8 ảnh.");
        }
        int selectedThumbnail = parseUploadThumbnail(form.getThumbnailSelection(), safeUploads.size());
        for (int i = 0; i < safeUploads.size(); i++) {
            CloudinaryImageService.UploadedImage upload = safeUploads.get(i);
            ProductImage image = new ProductImage();
            image.setProduct(saved);
            image.setImageUrl(upload.getImageUrl());
            image.setPublicId(upload.getPublicId());
            image.setDisplayOrder(i);
            image.setThumbnail(i == selectedThumbnail || (selectedThumbnail < 0 && i == 0));
            images.add(image);
        }
        productImageRepository.saveAll(images);
        return saved;
    }

    @Transactional
    public Product updateProduct(Long id, AdminProductForm form) {
        updateProduct(id, form, List.of());
        return getProductById(id);
    }

    @Transactional
    public List<String> updateProduct(
            Long id,
            AdminProductForm form,
            List<CloudinaryImageService.UploadedImage> uploadedImages
    ) {
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

        productRepository.save(product);
        return updateProductImages(product, form, uploadedImages);
    }

    @Transactional(readOnly = true)
    public List<ProductImage> getProductImages(Long productId) {
        return productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
    }

    @Transactional(readOnly = true)
    public void validateImageCount(Long productId, int uploadCount, List<Long> deleteImageIds) {
        List<ProductImage> currentImages =
                productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
        Set<Long> removeIds = new HashSet<>(
                deleteImageIds == null ? List.of() : deleteImageIds
        );
        long validRemovals = currentImages.stream()
                .filter(image -> removeIds.contains(image.getId()))
                .count();
        if (validRemovals != removeIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách ảnh cần xóa không hợp lệ.");
        }
        if (currentImages.size() + uploadCount - removeIds.size() > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mỗi sản phẩm được có tối đa 8 ảnh.");
        }
    }

    private List<String> updateProductImages(
            Product product,
            AdminProductForm form,
            List<CloudinaryImageService.UploadedImage> uploadedImages
    ) {
        List<ProductImage> currentImages =
                productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        List<CloudinaryImageService.UploadedImage> safeUploads =
                uploadedImages == null ? List.of() : uploadedImages;
        Set<Long> removeIds = new HashSet<>(
                form.getDeleteImageIds() == null ? List.of() : form.getDeleteImageIds()
        );
        List<ProductImage> removed = currentImages.stream()
                .filter(image -> removeIds.contains(image.getId()))
                .toList();
        if (removed.size() != removeIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách ảnh cần xóa không hợp lệ.");
        }
        List<ProductImage> remaining = currentImages.stream()
                .filter(image -> !removeIds.contains(image.getId()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (remaining.size() + safeUploads.size() > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mỗi sản phẩm được có tối đa 8 ảnh.");
        }

        int selectedUpload = parseUploadThumbnail(form.getThumbnailSelection(), safeUploads.size());
        Long selectedExisting = parseExistingThumbnail(form.getThumbnailSelection());
        boolean existingSelectionValid = selectedExisting != null
                && remaining.stream().anyMatch(image -> image.getId().equals(selectedExisting));
        if (selectedExisting != null && !existingSelectionValid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh đại diện được chọn không hợp lệ.");
        }

        ProductImage fallbackThumbnail = remaining.stream()
                .filter(ProductImage::isThumbnail)
                .findFirst()
                .orElseGet(() -> remaining.isEmpty() ? null : remaining.get(0));
        if (selectedUpload < 0 && selectedExisting == null && fallbackThumbnail == null
                && !safeUploads.isEmpty()) {
            selectedUpload = 0;
        }

        for (ProductImage image : remaining) {
            image.setThumbnail(selectedExisting != null
                    ? image.getId().equals(selectedExisting)
                    : selectedUpload < 0 && fallbackThumbnail != null
                    && image.getId().equals(fallbackThumbnail.getId()));
        }

        int nextOrder = currentImages.stream()
                .mapToInt(image -> image.getDisplayOrder() == null ? 0 : image.getDisplayOrder())
                .max()
                .orElse(-1) + 1;
        List<ProductImage> added = new ArrayList<>();
        for (int i = 0; i < safeUploads.size(); i++) {
            CloudinaryImageService.UploadedImage upload = safeUploads.get(i);
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl(upload.getImageUrl());
            image.setPublicId(upload.getPublicId());
            image.setDisplayOrder(nextOrder + i);
            image.setThumbnail(selectedUpload == i);
            added.add(image);
        }

        productImageRepository.deleteAll(removed);
        productImageRepository.saveAll(remaining);
        productImageRepository.saveAll(added);
        return removed.stream()
                .map(ProductImage::getPublicId)
                .filter(publicId -> publicId != null && !publicId.isBlank())
                .toList();
    }

    private int parseUploadThumbnail(String selection, int uploadCount) {
        if (selection == null || !selection.startsWith("upload-")) {
            return -1;
        }
        try {
            int index = Integer.parseInt(selection.substring("upload-".length()));
            if (index < 0 || index >= uploadCount) {
                throw new NumberFormatException();
            }
            return index;
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh đại diện được chọn không hợp lệ.");
        }
    }

    private Long parseExistingThumbnail(String selection) {
        if (selection == null || !selection.startsWith("existing-")) {
            return null;
        }
        try {
            return Long.parseLong(selection.substring("existing-".length()));
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh đại diện được chọn không hợp lệ.");
        }
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
    @Transactional
    public Brand createBrand(AdminBrandForm form) {
        String name = form.getName().trim();

        if (brandRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên hãng đã tồn tại."
            );
        }

        Brand brand = new Brand();
        brand.setName(name);
        brand.setLogoUrl(normalizeLogoUrl(form.getLogoUrl()));
        brand.setStatus(form.getStatus() != null ? form.getStatus() : BrandStatus.ACTIVE);

        return brandRepository.save(brand);
    }

    @Transactional
    public Brand updateBrand(Long id, AdminBrandForm form) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thương hiệu."));
        String name = form.getName().trim();
        if (!brand.getName().equalsIgnoreCase(name)
                && brandRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên hãng đã tồn tại.");
        }
        brand.setName(name);
        brand.setLogoUrl(normalizeLogoUrl(form.getLogoUrl()));
        if (form.getStatus() != null) {
            brand.setStatus(form.getStatus());
        }
        return brandRepository.save(brand);
    }

    @Transactional
    public Category createCategory(AdminCategoryForm form) {
        String name = form.getName().trim();

        String inputSlug = form.getSlug() != null
                ? form.getSlug().trim()
                : "";

        String slug = inputSlug.isEmpty()
                ? toSlug(name)
                : toSlug(inputSlug);

        if (slug.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể tạo slug từ tên danh mục."
            );
        }

        if (categoryRepository.existsBySlugIgnoreCase(slug)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Slug danh mục đã tồn tại."
            );
        }

        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(form.getDescription());
        category.setStatus(form.getStatus() != null ? form.getStatus() : CategoryStatus.ACTIVE);

        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(Long id, AdminCategoryForm form) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục."));
        String name = form.getName().trim();
        String inputSlug = form.getSlug() == null ? "" : form.getSlug().trim();
        String slug = inputSlug.isEmpty() ? toSlug(name) : toSlug(inputSlug);
        if (slug.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể tạo slug từ tên danh mục.");
        }
        if (!category.getSlug().equalsIgnoreCase(slug)
                && categoryRepository.existsBySlugIgnoreCaseAndIdNot(slug, id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug danh mục đã tồn tại.");
        }
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(form.getDescription());
        if (form.getStatus() != null) {
            category.setStatus(form.getStatus());
        }
        return categoryRepository.save(category);
    }

    private String normalizeLogoUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String url = value.trim();
        try {
            java.net.URI uri = java.net.URI.create(url);
            if (uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL logo phải bắt đầu bằng http:// hoặc https://.");
        }
        return url;
    }
}
