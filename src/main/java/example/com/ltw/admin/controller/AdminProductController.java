package example.com.ltw.admin.controller;

import example.com.ltw.admin.dto.AdminProductForm;
import example.com.ltw.admin.dto.AdminVariantForm;
import example.com.ltw.admin.dto.BranchInventoryForm;
import example.com.ltw.admin.dto.VariantInventoryDto;
import example.com.ltw.admin.service.AdminBranchService;
import example.com.ltw.admin.service.AdminProductService;
import example.com.ltw.admin.service.AdminUserService;
import example.com.ltw.branch.entity.Branch;
import example.com.ltw.catalog.entity.Product;
import example.com.ltw.catalog.entity.ProductStatus;
import example.com.ltw.catalog.entity.ProductVariant;
import example.com.ltw.catalog.entity.ProductVariantStatus;
import example.com.ltw.user.entity.User;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;
    private final AdminBranchService adminBranchService;
    private final AdminUserService adminUserService;

    // ==========================================
    // PRODUCT LIST & SEARCH
    // ==========================================

    @GetMapping
    public String listProducts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "brandId", required = false) Long brandId,
            @RequestParam(value = "status", required = false) ProductStatus status,
            HttpSession session,
            Model model
    ) {
        User admin = adminUserService.requireAdmin(session);

        model.addAttribute("admin", admin);
        model.addAttribute("products", adminProductService.getProducts(keyword, categoryId, brandId, status));
        model.addAttribute("brands", adminProductService.getAllBrands());
        model.addAttribute("categories", adminProductService.getAllCategories());
        model.addAttribute("statuses", ProductStatus.values());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedBrandId", brandId);
        model.addAttribute("selectedStatus", status);

        return "admin/product-list";
    }

    // ==========================================
    // CREATE PRODUCT
    // ==========================================

    @GetMapping("/new")
    public String showCreateProductForm(HttpSession session, Model model) {
        User admin = adminUserService.requireAdmin(session);

        AdminProductForm form = new AdminProductForm();
        form.setStatus(ProductStatus.ACTIVE);

        model.addAttribute("admin", admin);
        model.addAttribute("form", form);
        model.addAttribute("brands", adminProductService.getAllBrands());
        model.addAttribute("categories", adminProductService.getAllCategories());
        model.addAttribute("statuses", ProductStatus.values());
        model.addAttribute("isEdit", false);

        return "admin/product-form";
    }

    @PostMapping("/new")
    public String handleCreateProduct(
            @Valid @ModelAttribute("form") AdminProductForm form,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        User admin = adminUserService.requireAdmin(session);

        if (bindingResult.hasErrors()) {
            model.addAttribute("admin", admin);
            model.addAttribute("brands", adminProductService.getAllBrands());
            model.addAttribute("categories", adminProductService.getAllCategories());
            model.addAttribute("statuses", ProductStatus.values());
            model.addAttribute("isEdit", false);
            return "admin/product-form";
        }

        try {
            Product saved = adminProductService.createProduct(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Thêm sản phẩm '" + saved.getName() + "' thành công! Bạn có thể thêm các biến thể cấu hình bên dưới.");
            return "redirect:/admin/products/" + saved.getId() + "/edit";
        } catch (ResponseStatusException ex) {
            model.addAttribute("admin", admin);
            model.addAttribute("errorMessage", ex.getReason());
            model.addAttribute("brands", adminProductService.getAllBrands());
            model.addAttribute("categories", adminProductService.getAllCategories());
            model.addAttribute("statuses", ProductStatus.values());
            model.addAttribute("isEdit", false);
            return "admin/product-form";
        }
    }

    // ==========================================
    // EDIT PRODUCT & MANAGE VARIANTS
    // ==========================================

    @GetMapping("/{id}/edit")
    public String showEditProductForm(
            @PathVariable Long id,
            HttpSession session,
            Model model
    ) {
        User admin = adminUserService.requireAdmin(session);
        Product product = adminProductService.getProductById(id);

        AdminProductForm form = new AdminProductForm();
        form.setId(product.getId());
        form.setName(product.getName());
        form.setSlug(product.getSlug());
        form.setDescription(product.getDescription());
        form.setOperatingSystem(product.getOperatingSystem());
        form.setChipset(product.getChipset());
        form.setScreenSize(product.getScreenSize());
        form.setBatteryCapacity(product.getBatteryCapacity());
        form.setBrandId(product.getBrand().getId());
        form.setCategoryId(product.getCategory().getId());
        form.setStatus(product.getStatus());

        List<ProductVariant> variants = adminProductService.getVariantsByProductId(id);

        AdminVariantForm variantForm = new AdminVariantForm();
        variantForm.setProductId(id);
        variantForm.setStatus(ProductVariantStatus.ACTIVE);

        model.addAttribute("admin", admin);
        model.addAttribute("form", form);
        model.addAttribute("product", product);
        model.addAttribute("variants", variants);
        model.addAttribute("variantForm", variantForm);
        model.addAttribute("brands", adminProductService.getAllBrands());
        model.addAttribute("categories", adminProductService.getAllCategories());
        model.addAttribute("statuses", ProductStatus.values());
        model.addAttribute("variantStatuses", ProductVariantStatus.values());
        model.addAttribute("isEdit", true);

        return "admin/product-form";
    }

    @PostMapping("/{id}/edit")
    public String handleEditProduct(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") AdminProductForm form,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        User admin = adminUserService.requireAdmin(session);

        if (bindingResult.hasErrors()) {
            model.addAttribute("admin", admin);
            model.addAttribute("product", adminProductService.getProductById(id));
            model.addAttribute("variants", adminProductService.getVariantsByProductId(id));
            model.addAttribute("brands", adminProductService.getAllBrands());
            model.addAttribute("categories", adminProductService.getAllCategories());
            model.addAttribute("statuses", ProductStatus.values());
            model.addAttribute("variantStatuses", ProductVariantStatus.values());
            model.addAttribute("isEdit", true);
            return "admin/product-form";
        }

        try {
            adminProductService.updateProduct(id, form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật thông tin sản phẩm thành công!");
            return "redirect:/admin/products/" + id + "/edit";
        } catch (ResponseStatusException ex) {
            model.addAttribute("admin", admin);
            model.addAttribute("errorMessage", ex.getReason());
            model.addAttribute("product", adminProductService.getProductById(id));
            model.addAttribute("variants", adminProductService.getVariantsByProductId(id));
            model.addAttribute("brands", adminProductService.getAllBrands());
            model.addAttribute("categories", adminProductService.getAllCategories());
            model.addAttribute("statuses", ProductStatus.values());
            model.addAttribute("variantStatuses", ProductVariantStatus.values());
            model.addAttribute("isEdit", true);
            return "admin/product-form";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleProductStatus(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        adminUserService.requireAdmin(session);
        try {
            ProductStatus newStatus = adminProductService.toggleProductStatus(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Trạng thái sản phẩm chuyển sang: " + newStatus);
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getReason());
        }
        return "redirect:/admin/products";
    }

    // ==========================================
    // VARIANTS
    // ==========================================

    @PostMapping("/{id}/variants")
    public String handleAddVariant(
            @PathVariable Long id,
            @Valid @ModelAttribute("variantForm") AdminVariantForm variantForm,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        adminUserService.requireAdmin(session);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Dữ liệu biến thể không hợp lệ: " + bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/products/" + id + "/edit";
        }

        try {
            adminProductService.addVariant(id, variantForm);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm biến thể thành công!");
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getReason());
        }

        return "redirect:/admin/products/" + id + "/edit";
    }

    @PostMapping("/variants/{variantId}/edit")
    public String handleUpdateVariant(
            @PathVariable Long variantId,
            @Valid @ModelAttribute("variantForm") AdminVariantForm variantForm,
            BindingResult bindingResult,
            @RequestParam("productId") Long productId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        adminUserService.requireAdmin(session);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Dữ liệu biến thể không hợp lệ: " + bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/products/" + productId + "/edit";
        }

        try {
            adminProductService.updateVariant(variantId, variantForm);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật biến thể thành công!");
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getReason());
        }

        return "redirect:/admin/products/" + productId + "/edit";
    }

    // ==========================================
    // ALLOCATION TO BRANCHES
    // ==========================================

    @GetMapping("/allocate")
    public String showAllocationPage(
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "keyword", required = false) String keyword,
            HttpSession session,
            Model model
    ) {
        User admin = adminUserService.requireAdmin(session);

        List<Branch> branches = adminBranchService.getAllBranches();
        List<Product> products = adminProductService.getProducts(keyword, null, null, ProductStatus.ACTIVE);

        List<VariantInventoryDto> variantsWithInventory = Collections.emptyList();
        Product selectedProduct = null;
        Branch selectedBranch = null;

        if (branchId != null && productId != null) {
            variantsWithInventory = adminProductService.getVariantsForAllocation(productId, branchId);
            selectedProduct = adminProductService.getProductById(productId);
            selectedBranch = adminBranchService.getBranchById(branchId);
        }

        BranchInventoryForm form = new BranchInventoryForm();
        form.setBranchId(branchId);
        form.setProductId(productId);

        model.addAttribute("admin", admin);
        model.addAttribute("branches", branches);
        model.addAttribute("products", products);
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("selectedProductId", productId);
        model.addAttribute("selectedProduct", selectedProduct);
        model.addAttribute("selectedBranch", selectedBranch);
        model.addAttribute("keyword", keyword);
        model.addAttribute("variantsWithInventory", variantsWithInventory);
        model.addAttribute("form", form);

        return "admin/product-allocation";
    }

    @PostMapping("/allocate")
    public String handleAllocateInventory(
            @Valid @ModelAttribute("form") BranchInventoryForm form,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        adminUserService.requireAdmin(session);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Dữ liệu phân bổ không hợp lệ: " + bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/products/allocate?branchId=" + form.getBranchId()
                    + (form.getProductId() != null ? "&productId=" + form.getProductId() : "");
        }

        try {
            adminProductService.allocateBranchInventory(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã cập nhật tồn kho biến thể tại chi nhánh thành công!");
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getReason());
        }

        return "redirect:/admin/products/allocate?branchId=" + form.getBranchId()
                + (form.getProductId() != null ? "&productId=" + form.getProductId() : "");
    }

    // JSON API for dynamic UI frontend
    @GetMapping("/api/variants")
    @ResponseBody
    public ResponseEntity<List<VariantInventoryDto>> getVariantsForBranch(
            @RequestParam("productId") Long productId,
            @RequestParam("branchId") Long branchId,
            HttpSession session
    ) {
        adminUserService.requireAdmin(session);
        List<VariantInventoryDto> variants = adminProductService.getVariantsForAllocation(productId, branchId);
        return ResponseEntity.ok(variants);
    }
}
